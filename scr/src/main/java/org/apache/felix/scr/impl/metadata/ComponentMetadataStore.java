/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.scr.impl.metadata;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public class ComponentMetadataStore
{
    // requirement that finds all capabilities in the bundle namespace
    private static Requirement ALL_IDENTITY_REQUIREMENT = new Requirement()
    {
        private final Map<String, String> directives = Collections.singletonMap(
            Namespace.REQUIREMENT_FILTER_DIRECTIVE,
            "(" + IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE + "="
                + IdentityNamespace.TYPE_BUNDLE + ")");

        public String getNamespace()
        {
            return IdentityNamespace.IDENTITY_NAMESPACE;
        }

        public Map<String, String> getDirectives()
        {
            return directives;
        }

        public Map<String, Object> getAttributes()
        {
            return Collections.emptyMap();
        }

        public Resource getResource()
        {
            return null;
        }
    };

    private final Map<BundleWiringLastModified, List<ComponentMetadata>> m_metaDataCache = Collections.synchronizedMap(
        new HashMap<BundleWiringLastModified, List<ComponentMetadata>>());

    public List<ComponentMetadata> getMetadata(Bundle bundle)
    {
        BundleWiring wiring = bundle.adapt(BundleWiring.class);
        return wiring == null ? null
            : m_metaDataCache.get(new BundleWiringLastModified(wiring));
    }

    public void addMetadata(Bundle bundle, List<ComponentMetadata> metadata)
    {
        BundleWiring wiring = bundle.adapt(BundleWiring.class);
        if (wiring != null)
        {
            m_metaDataCache.put(new BundleWiringLastModified(wiring), metadata);
        }
    }

    @SuppressWarnings("unchecked")
    public void load(File cacheFile)
        throws ClassNotFoundException, IOException
    {
        if (cacheFile.isFile())
        {
            ObjectInputStream ois = null;
            try
            {
                ois = new ObjectInputStream(new FileInputStream(cacheFile));
                synchronized (m_metaDataCache)
                {

                    m_metaDataCache.clear();
                    m_metaDataCache.putAll((Map<? extends BundleWiringLastModified, ? extends List<ComponentMetadata>>) ois.readObject());
                }
            }
            finally
            {
                if (ois != null)
                {
                    try
                    {
                        ois.close();
                    }
                    catch (IOException e)
                    {
                        // ignore
                    }
                }
            }
        }
    }

    public void save(File cacheFile, FrameworkWiring fwkWiring) throws IOException
    {
        Set<BundleWiringLastModified> currentLastModified = getCurrentLastModified(
            fwkWiring);
        synchronized (m_metaDataCache)
        {
            m_metaDataCache.keySet().retainAll(currentLastModified);
            cacheFile.getParentFile().mkdirs();
            ObjectOutputStream oos = null;
            try
            {
                oos = new ObjectOutputStream(new FileOutputStream(cacheFile));
                oos.writeObject(m_metaDataCache);
            }
            finally
            {
                if (oos != null)
                {
                    try
                    {
                        oos.close();
                    }
                    catch (IOException e)
                    {
                        // ignore
                    }
                }
            }
        }
    }

    private Set<BundleWiringLastModified> getCurrentLastModified(
        FrameworkWiring fwkWiring)
    {
        Set<BundleWiringLastModified> lastModified = new HashSet<BundleWiringLastModified>();
        Collection<BundleCapability> bundles = fwkWiring.findProviders(
            ALL_IDENTITY_REQUIREMENT);
        for (BundleCapability bundleCap : bundles)
        {
            BundleRevision revision = bundleCap.getRevision();
            BundleWiring wiring = revision.getWiring();
            if (wiring != null && wiring.isCurrent())
            {
                lastModified.add(new BundleWiringLastModified(wiring));
            }
        }
        return lastModified;
    }
}
