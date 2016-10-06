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
package org.apache.felix.scr.impl.inject;


import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.impl.helper.SimpleLogger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.log.LogService;


/**
 * Utility methods for class handling used by method and field references.
 */
public class ClassUtils
{
    private static final Class<?> OBJECT_CLASS = Object.class;

    public static final Class<?> SERVICE_REFERENCE_CLASS = ServiceReference.class;

    public static final Class<?> COMPONENTS_SERVICE_OBJECTS_CLASS = ComponentServiceObjects.class;

    public static final Class<?> MAP_CLASS = Map.class;
    public static final Class<?> MAP_ENTRY_CLASS = Map.Entry.class;

    public static final Class<?> COLLECTION_CLASS = Collection.class;
    public static final Class<?> LIST_CLASS = List.class;

    // this bundle's context
    private static BundleContext m_context;

    public static volatile FrameworkWiring m_fwkWiring;

    /**
     * Returns the class object representing the class of the field reference
     * The class loader of the component class is used to load the service class.
     * <p>
     * It may well be possible, that the class loader of the target class cannot
     * see the service object class, for example if the service reference is
     * inherited from a component class of another bundle.
     *
     * @return The class object for the referred to service or <code>null</code>
     *      if the class loader of the <code>targetClass</code> cannot see that
     *      class.
     */
    public static Class<?> getClassFromComponentClassLoader(
            final Class<?> componentClass,
            final String className,
            final SimpleLogger logger )
    {
        if ( logger.isLogEnabled( LogService.LOG_DEBUG ) )
        {
            logger.log(
                LogService.LOG_DEBUG,
                "getReferenceClass: Looking for interface class {0} through loader of {1}",
                    new Object[] {className, componentClass.getName()}, null );
        }

        try
        {
            // need the class loader of the target class, which may be the
            // system classloader, which case getClassLoader may retur null
            ClassLoader loader = componentClass.getClassLoader();
            if ( loader == null )
            {
                loader = ClassLoader.getSystemClassLoader();
            }

            final Class<?> referenceClass = loader.loadClass( className );
            if ( logger.isLogEnabled( LogService.LOG_DEBUG ) )
            {
                logger.log( LogService.LOG_DEBUG,
                    "getParameterClass: Found class {0}", new Object[] {referenceClass.getName()}, null );
            }
            return referenceClass;
        }
        catch ( final ClassNotFoundException cnfe )
        {
            // if we can't load the class, perhaps the method is declared in a
            // super class so we try this class next
        }

        if ( logger.isLogEnabled( LogService.LOG_DEBUG ) )
        {
            logger.log( LogService.LOG_DEBUG,
                "getParameterClass: Not found through component class, using PackageAdmin service", null );
        }

        // try to load the class with the help of the FrameworkWiring
        Class<?> referenceClass = findClassFromFrameworkWiring(className, logger);
        if (referenceClass != null)
        {
            return referenceClass;
        }

        // class cannot be found, neither through the component nor from an
        // export, so we fall back to assuming Object
        if (logger.isLogEnabled(LogService.LOG_DEBUG))
        {
            logger.log(LogService.LOG_DEBUG,
                "getParameterClass: No class found, falling back to class Object", null);
        }
        return OBJECT_CLASS;
    }

    private static Class<?> findClassFromFrameworkWiring(String className,
        SimpleLogger logger)
    {
        FrameworkWiring fwkWiring = getFrameworkWiring();
        if (fwkWiring == null)
        {
            if (logger.isLogEnabled(LogService.LOG_DEBUG))
            {
                logger.log(LogService.LOG_DEBUG,
                    "getParameterClass: FrameworkWiring not available, cannot find class",
                    null);
            }
            return null;
        }

        final String classPackage = className.substring(0, className.lastIndexOf('.'));
        final String classPackageFilter = "(" + PackageNamespace.PACKAGE_NAMESPACE + "="
            + classPackage + ")";
        final Requirement pkgReq = new Requirement()
        {

            public Resource getResource()
            {
                return null;
            }

            public String getNamespace()
            {
                return PackageNamespace.PACKAGE_NAMESPACE;
            }

            public Map<String, String> getDirectives()
            {
                return Collections.singletonMap(PackageNamespace.PACKAGE_NAMESPACE,
                    classPackageFilter);
            }

            public Map<String, Object> getAttributes()
            {
                return Collections.emptyMap();
            }
        };

        Collection<BundleCapability> packageCaps = fwkWiring.findProviders(pkgReq);
        for (BundleCapability packageCap : packageCaps)
        {
            try
            {
                BundleRevision revision = packageCap.getRevision();
                if (logger.isLogEnabled(LogService.LOG_DEBUG))
                {
                    logger.log(LogService.LOG_DEBUG,
                        "getParameterClass: Checking Bundle {0}/{1}",
                        new Object[] { revision.getSymbolicName(),
                                revision.getBundle().getBundleId() },
                        null);
                }
                BundleWiring wiring = revision.getWiring();
                if (wiring != null)
                {
                    Class<?> referenceClass = loadClass(wiring, className);
                    if (logger.isLogEnabled(LogService.LOG_DEBUG))
                    {
                        logger.log(LogService.LOG_DEBUG,
                            "getParameterClass: Found class {0}",
                            new Object[] { referenceClass.getName() }, null);
                    }
                    return referenceClass;
                }
            }
            catch (ClassNotFoundException cnfe)
            {
                // exported package does not provide the interface !!!!
            }
        }
        if (logger.isLogEnabled(LogService.LOG_DEBUG))
        {
            logger.log( LogService.LOG_DEBUG,
            "getParameterClass: No bundles exporting package {0} found",
                new Object[] { classPackage }, null);
        }
        return null;
    }


    private static Class<?> loadClass(BundleWiring wiring, String className)
        throws ClassNotFoundException
    {
        if ((wiring.getRevision().getTypes() & BundleRevision.TYPE_FRAGMENT) != 0)
        {
            // fragment case; just use first host
            List<BundleWire> hostWires = wiring.getRequiredWires(
                HostNamespace.HOST_NAMESPACE);
            if (hostWires == null)
            {
                // this happens if the wiring got flushed before we could get
                // the class loader.
                throw new ClassNotFoundException(className);
            }
            wiring = hostWires.get(0).getProviderWiring();
        }
        ClassLoader loader = wiring.getClassLoader();
        if (loader != null)
        {
            // again can happen if the wiring got flushed befoer we got here.
            return loader.loadClass(className);
        }
        throw new ClassNotFoundException(className);
    }

    public static void setBundleContext( BundleContext bundleContext )
    {
        ClassUtils.m_context = bundleContext;
    }

    private static FrameworkWiring getFrameworkWiring()
    {
        if (m_fwkWiring == null)
        {
            synchronized (ClassUtils.class)
            {
                if (m_fwkWiring == null)
                {
                    m_fwkWiring = m_context.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).adapt(FrameworkWiring.class);
                }
            }
        }

        return m_fwkWiring;
    }

    public static void close()
    {
        m_fwkWiring = null;

        // remove the reference to the component context
        m_context = null;
    }
}
