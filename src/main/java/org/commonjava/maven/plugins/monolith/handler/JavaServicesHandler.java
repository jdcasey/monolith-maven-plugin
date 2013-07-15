package org.commonjava.maven.plugins.monolith.handler;

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

import org.codehaus.plexus.components.io.fileselectors.FileInfo;
import org.codehaus.plexus.logging.Logger;
import org.commonjava.maven.plugins.monolith.comp.MonolithVersioningContext;

public class JavaServicesHandler
    extends AbstractAggregatingHandler
{

    private static final String SERVICES_PATH_PREFIX = "META-INF/services/";

    public JavaServicesHandler( final MonolithVersioningContext context, final Logger logger )
    {
        super( context, logger );
    }

    @Override
    protected String getOutputPathPrefix( final FileInfo fileInfo )
    {
        return SERVICES_PATH_PREFIX;
    }

    @Override
    protected boolean fileMatches( final FileInfo fileInfo )
    {
        final String path = fileInfo.getName();

        String leftover = null;
        if ( path.startsWith( SERVICES_PATH_PREFIX ) )
        {
            leftover = path.substring( SERVICES_PATH_PREFIX.length() );
        }
        else if ( path.startsWith( "/META-INF/services/" ) )
        {
            leftover = path.substring( SERVICES_PATH_PREFIX.length() - 1 );
        }

        return leftover != null && leftover.length() > 0;
    }

}
