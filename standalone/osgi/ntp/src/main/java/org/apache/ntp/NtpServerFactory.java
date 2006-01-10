/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.apache.ntp;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.apache.mina.registry.ServiceRegistry;
import org.apache.protocol.common.MapAdapter;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ungoverned.gravity.servicebinder.Lifecycle;

public class NtpServerFactory implements ManagedServiceFactory, Lifecycle
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( NtpServerFactory.class );

    private static final String DEFAULT_PID = "org.apache.ntp.default";

    private ConfigurationAdmin cm;
    private ServiceRegistry registry;

    private Map servers = Collections.synchronizedMap( new HashMap() );

    public void updated( String pid, Dictionary config ) throws ConfigurationException
    {
        log.debug( getName() + " (" + pid + ") updating with " + config );

        NtpConfiguration ntpConfig = new NtpConfiguration( new MapAdapter( config ) );

        synchronized ( servers )
        {
            if ( pid.equals( DEFAULT_PID ) && servers.size() > 0 )
            {
                return;
            }

            // As soon as we get a non-default config, delete the default.
            if ( !pid.equals( DEFAULT_PID ) )
            {
                deleted( DEFAULT_PID );
            }

            // For a given pid, do we have the service?
            NtpServer ntpServer = (NtpServer) servers.get( pid );

            if ( ntpServer != null )
            {
                log.debug( "isDifferent" + ntpServer.isDifferent( config ) );
            }

            // If we don't have the service, create it with the config.
            // Or, if we do have the service, re-create it if the config is different.
            if ( ntpServer == null || ntpServer.isDifferent( config ) )
            {
                deleted( pid );
                ntpServer = new NtpServer( ntpConfig, registry );
                servers.put( pid, ntpServer );
            }
        }
    }

    public void deleted( String pid )
    {
        synchronized ( servers )
        {
            NtpServer ntpServer = (NtpServer) servers.remove( pid );

            if ( ntpServer != null )
            {
                ntpServer.destroy();
            }
        }
    }

    public String getName()
    {
        return "Apache NTP Service Factory";
    }

    /**
     * All required services have been bound, but our service(s) are not yet
     * registered.  If there is no Config Admin we start a server with default properties.
     */
    public void activate()
    {
        try
        {
            if ( cm == null )
            {
                updated( DEFAULT_PID, new Hashtable( NtpConfiguration.getDefaultConfig() ) );
            }
        }
        catch ( Exception e )
        {
            log.error( e.getMessage(), e );
        }
        finally
        {
            cm = null;
        }
    }

    /**
     * Invalidation has started and our services have been unregistered, but
     * any required services have not been unbound yet.
     */
    public void deactivate()
    {
        synchronized ( servers )
        {
            Iterator it = servers.values().iterator();

            while ( it.hasNext() )
            {
                NtpServer ntpServer = (NtpServer) it.next();
                ntpServer.destroy();
            }

            servers.clear();
        }
    }

    public void setServiceRegistry( ServiceRegistry registry )
    {
        this.registry = registry;
        log.debug( getName() + " has bound to " + registry );
    }

    public void unsetServiceRegistry( ServiceRegistry registry )
    {
        this.registry = null;
        log.debug( getName() + " has unbound from " + registry );
    }

    public void setConfigurationAdmin( ConfigurationAdmin cm )
    {
        this.cm = cm;
        log.debug( getName() + " has bound to " + cm );
    }

    public void unsetConfigurationAdmin( ConfigurationAdmin cm )
    {
        this.cm = null;
        log.debug( getName() + " has unbound from " + cm );
    }
}
