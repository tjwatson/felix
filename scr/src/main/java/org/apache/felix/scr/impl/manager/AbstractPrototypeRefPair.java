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

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

/**
 * @version $Rev$ $Date$
 */
public abstract class AbstractPrototypeRefPair<S, T> extends RefPair<S, T>
{
    public AbstractPrototypeRefPair( ServiceReference<T> ref )
    {
        super(ref);
    }

    @Override
    public abstract T getServiceObject(ComponentContextImpl<S> key);

    @Override
    public abstract boolean setServiceObject(ComponentContextImpl<S> key, T serviceObject);

    protected abstract T remove(ComponentContextImpl<S> key);

    protected abstract Collection<Entry<ComponentContextImpl<S>, T>> clearEntries();

    @Override
    public final T ungetServiceObject(ComponentContextImpl<S> key)
    {
        if ( key == null )
        {
            Collection<Map.Entry<ComponentContextImpl<S>,T>> keys = clearEntries();
            for (Map.Entry<ComponentContextImpl<S>,T> e : keys)
            {
                doUngetService( e.getKey(), e.getValue() );
            }
            return null ;
        }
        T service = remove( key );
        if(service != null) {
        	doUngetService( key, service );
        }
		return service;
    }

    @Override
    public final void ungetServiceObjects(BundleContext bundleContext) {
        ungetServiceObject(null);
    }

    @Override
    public abstract String toString();

    @Override
    public final boolean getServiceObject(ComponentContextImpl<S> key, BundleContext context)
    {
        final T service = key.getComponentServiceObjectsHelper().getPrototypeRefInstance(this.getRef());
        if ( service == null )
        {
            setFailed();
            key.getLogger().log(
                 LogService.LOG_WARNING,
                 "Could not get service from serviceobjects for ref {0}", null, getRef() );
            return false;
        }
        if (!setServiceObject(key, service))
        {
            // Another thread got the service before, so unget our
        	doUngetService( key, service );
        }
        return true;
    }

	@SuppressWarnings("unchecked")
    private void doUngetService(ComponentContextImpl<S> key, final T service) {
		try 
		{
			key.getComponentServiceObjectsHelper().getServiceObjects(getRef()).ungetService( service );
		}
		catch ( final IllegalStateException ise )
		{
			// ignore
		}
	}
}
