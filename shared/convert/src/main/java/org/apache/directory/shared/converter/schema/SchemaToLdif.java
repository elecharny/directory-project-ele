
package org.apache.directory.shared.converter.schema;

import java.io.InputStream;
import java.io.Writer;
import java.util.List;

import org.apache.directory.shared.converter.schema.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchemaToLdif
{
    private static final String HEADER = 
        "#\n" +
        "#  Licensed to the Apache Software Foundation (ASF) under one\n" +
        "#  or more contributor license agreements.  See the NOTICE file\n" +
        "#  distributed with this work for additional information\n" +
        "#  regarding copyright ownership.  The ASF licenses this file\n" +
        "#  to you under the Apache License, Version 2.0 (the\n" +
        "#  \"License\"); you may not use this file except in compliance\n" +
        "#  with the License.  You may obtain a copy of the License at\n" +
        "#  \n" +
        "#    http://www.apache.org/licenses/LICENSE-2.0\n" +
        "#  \n" +
        "#  Unless required by applicable law or agreed to in writing,\n" +
        "#  software distributed under the License is distributed on an\n" +
        "#  \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n" +
        "#  KIND, either express or implied.  See the License for the\n" +
        "#  specific language governing permissions and limitations\n" +
        "#  under the License. \n" +
        "#\n" +
        "version: 1\n" +
        "\n";  

    /** The logger */
    private static Logger log = LoggerFactory.getLogger( SchemaToLdif.class );


    public void transform( List<Schema> schemas ) throws ParserException
    {
        // Bypass if no schemas have yet been defined 
        if ( schemas == null || schemas.size() == 0 )
        {
            log.warn( "No schemas defined!" );
            return;
        }

        // Make sure schema configurations have a name field and set defaults
        // for any other missing properties of the bean: pkg and owner.
        int i = 1;
        
        for ( Schema schema:schemas )
        {
            if ( schema.getName() == null )
            {
                String msg = i + "th schema configuration element must specify a name.";
                log.error( msg );
                throw new ParserException( msg );
            }

        }

        // Generate for each schema 
        for ( Schema schema:schemas )
        {
            try
            {
                log.info( "Generating " + schema.getName() + " schema." );
                generate( schema );
            }
            catch ( Exception e )
            {
                throw new ParserException( "Failed while generating sources for " + schema.getName() );
            }
        }
    }
    
    private void generate( Schema schema ) throws Exception
    {
        if ( schema == null )
        {
            log.error( "Can't generate a ldif for a null schema" );
            throw new NullPointerException( "the schema property must be set" );
        }

        InputStream in = schema.getInput();
        Writer out = schema.getOutput();
        
        SchemaParser parser = new SchemaParser();
        parser.parse( in, out );
    }
}
