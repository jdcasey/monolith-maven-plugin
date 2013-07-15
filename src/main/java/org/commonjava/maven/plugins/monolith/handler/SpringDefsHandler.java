package org.commonjava.maven.plugins.monolith.handler;

import org.codehaus.plexus.components.io.fileselectors.FileInfo;
import org.codehaus.plexus.logging.Logger;
import org.commonjava.maven.plugins.monolith.comp.MonolithVersioningContext;

public class SpringDefsHandler
    extends AbstractAggregatingHandler
{

    private static final String SPRING_PATH_PREFIX = "META-INF/";

    public SpringDefsHandler( final MonolithVersioningContext monolithVersioningContext, final Logger logger )
    {
        super( monolithVersioningContext, logger );
    }

    @Override
    protected String getOutputPathPrefix( final FileInfo fileInfo )
    {
        return SPRING_PATH_PREFIX;
    }

    @Override
    protected boolean fileMatches( final FileInfo fileInfo )
    {
        final String path = fileInfo.getName();

        String leftover = null;
        if ( path.startsWith( "/META-INF/spring." ) )
        {
            leftover = path.substring( "/META-INF/spring.".length() );
        }
        else if ( path.startsWith( "META-INF/spring." ) )
        {
            leftover = path.substring( "META-INF/spring.".length() - 1 );
        }

        return leftover != null && leftover.length() > 0;
    }

}
