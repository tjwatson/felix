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
package org.apache.felix.scr.impl.inject.methods;


import org.apache.felix.scr.impl.inject.InitReferenceMethod;
import org.apache.felix.scr.impl.inject.ReferenceMethod;
import org.apache.felix.scr.impl.inject.ReferenceMethods;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;

/**
 * @version $Rev$ $Date$
 */
public class BindMethods implements ReferenceMethods
{
    private final ReferenceMethod m_bind;
    private final ReferenceMethod m_updated;
    private final ReferenceMethod m_unbind;

    public BindMethods(ComponentMetadata componentMetadata, ReferenceMetadata dependencyMetadata, Class<?> instanceClass)
    {
        m_bind = new BindMethod(instanceClass, componentMetadata, dependencyMetadata);

        m_updated = new UpdatedMethod(instanceClass, componentMetadata,
            dependencyMetadata);

        m_unbind = new UnbindMethod(instanceClass, componentMetadata, dependencyMetadata);
    }

    public ReferenceMethod getBind()
    {
        return m_bind;
    }

    public ReferenceMethod getUnbind()
    {
        return m_unbind;
    }

    public ReferenceMethod getUpdated()
    {
        return m_updated;
    }

    public InitReferenceMethod getInit()
    {
        return null;
    }
}
