/*
 *   Copyright 2004 The Apache Software Foundation
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
package org.apache.directory.server.standalone.daemon;


import org.slf4j.LoggerFactory;
import org.slf4j.Logger;


/**
 * The main() based application bootstrapper used as the entry point for the 
 * executable bootstrapper.jar so it can be launched as a simple java application.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class MainBootstrapper
{
    private static final Logger log = LoggerFactory.getLogger( MainBootstrapper.class );
    private static final String[] EMPTY_STRARRAY = new String[0];


    public static void main( String[] args )
    {
        log.debug( "main(String[]) called" );
        
        // Noticed that some starts with jar2exe.exe pass in a null arguement list
        if ( args == null )
        {
            System.err.println( "Arguements are null - how come?" );
            log.error( "main() args were null shutting down!" );
            printHelp();
            System.exit( ExitCodes.BAD_ARGUMENTS );
        }

        if ( log.isDebugEnabled() )
        {
            log.debug( "main() recieved args:" );
            for ( int ii = 0; ii < args.length; ii++ )
            {
                log.debug( "\targs[" + ii + "] = " + args[ii] );
            }
        }

        LifecycleInvoker invoker = null;
        if ( args.length > 1 )
        {
        	log.debug( "main(String[]) creating LifecycleInvoker ... )" );
            invoker = new LifecycleInvoker( args[0], Thread.currentThread().getContextClassLoader() );
        }
        else
        {
            String msg = "Server exiting without required installation.home or command.name.";
            System.err.println( msg );
            log.error( msg );
            printHelp();
            System.exit( 1 );
        }

        String command = args[args.length - 1];
        try
        {
            if ( command.equalsIgnoreCase( "start" ) )
            {
                log.debug( "calling application.callInit(String[]) from main(String[])" );
                if ( args.length > 2 )
                {
                    String[] shifted = new String[args.length-2];
                    System.arraycopy( args, 2, shifted, 0, shifted.length );
                    invoker.callInit( shifted );
                }
                else
                {
                    invoker.callInit( EMPTY_STRARRAY );
                }

                log.debug( "calling application.callStart(String[]) from main(String[])" );
                invoker.callStart( true );
            }
            else if ( command.equalsIgnoreCase( "stop" ) )
            {
                log.debug( "calling application.callStop(String[]) from main(String[])" );
                if ( args.length > 2 )
                {
                    String[] shifted = new String[args.length-2];
                    System.arraycopy( args, 2, shifted, 0, shifted.length );
                    invoker.callStop( shifted );
                }
                else
                {
                    invoker.callStop( EMPTY_STRARRAY );
                }
                log.debug( "calling application.callDestroy() from main(String[])" );
                invoker.callDestroy();
            }
            else
            {
                log.error( "Unrecognized command " + command );
                printHelp();
                System.exit( ExitCodes.BAD_COMMAND );
            }
        }
        catch ( Throwable t )
        {
        	log.error( "Encountered error while processing command: " + command, t );
            System.exit( ExitCodes.UNKNOWN );
        }
    }


    private static void printHelp()
    {
        System.err.println("java -jar bootstrapper.jar <install.home> [start|stop] [apparg0 apparg1 ... appargN]");
    }
}
