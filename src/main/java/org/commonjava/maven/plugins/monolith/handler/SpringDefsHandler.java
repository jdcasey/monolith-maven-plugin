package org.commonjava.maven.plugins.monolith.handler;

import org.apache.maven.plugin.assembly.filter.ContainerDescriptorHandler;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.components.io.fileselectors.FileInfo;

@Component( role = ContainerDescriptorHandler.class, hint = SpringDefsHandler.ID, instantiationStrategy = "per-lookup" )
public class SpringDefsHandler
    extends AbstractAggregatingHandler
{

    public static final String ID = "spring-defs";

    private static final String SPRING_PATH_PREFIX = "META-INF/";

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
