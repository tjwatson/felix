/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.apache.felix.scr.impl.manager;

import java.util.concurrent.atomic.AtomicReference;

import org.apache.felix.scr.impl.inject.RefPair;
import org.apache.felix.scr.impl.inject.ScrComponentContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

/**
 * @version $Rev$ $Date$
 */
public class SingleRefPair<S, T> extends RefPair<S, T>
{
    private final AtomicReference<T> serviceObjectRef = new AtomicReference<>();

    public SingleRefPair( ServiceReference<T> ref )
    {
        super(ref);
    }

    @Override
    public T getServiceObject(ScrComponentContext key)
    {
        return serviceObjectRef.get();
    }

    @Override
    public boolean setServiceObject(ScrComponentContext key, T serviceObject)
    {
        boolean set = serviceObjectRef.compareAndSet( null, serviceObject );
        if ( serviceObject != null)
        {
            clearFailed();
        }
        return set;
    }

    @Override
    public T ungetServiceObject(ScrComponentContext key) {
        // null operation for singleRefPair
        return null;
    }

    @Override
    public void ungetServiceObjects(BundleContext bundleContext)
    {
        T service = serviceObjectRef.getAndSet( null );
        if (service != null)
        {
            if (bundleContext != null)
            {
                bundleContext.ungetService(getRef());
            }
        }
    }

    @Override
    public String toString()
    {
        return "[RefPair: ref: [" + getRef() + "] service: [" + serviceObjectRef.get() + "]]";
    }

    @Override
    public boolean getServiceObject(ScrComponentContext key, BundleContext context)
    {
        T service = context.getService( getRef() );
        if ( service == null )
        {
            markFailed();
            key.getLogger().log(
                 LogService.LOG_WARNING,
                 "Could not get service from ref {0}", null, getRef() );
            return false;
        }
        if (!setServiceObject(key, service))
        {
            // Another thread got the service before, so unget our
            context.ungetService( getRef() );
        }
        return true;
    }
}
