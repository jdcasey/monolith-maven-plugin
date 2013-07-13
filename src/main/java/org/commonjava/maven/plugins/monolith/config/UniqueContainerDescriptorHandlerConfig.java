package org.commonjava.maven.plugins.monolith.config;

import org.apache.maven.plugin.assembly.model.ContainerDescriptorHandlerConfig;

public class UniqueContainerDescriptorHandlerConfig
    extends ContainerDescriptorHandlerConfig
{

    private static final long serialVersionUID = 1L;

    public UniqueContainerDescriptorHandlerConfig( final String handlerName )
    {
        setHandlerName( handlerName );
    }

    public UniqueContainerDescriptorHandlerConfig( final ContainerDescriptorHandlerConfig config )
    {
        setHandlerName( config.getHandlerName() );
        setConfiguration( config.getConfiguration() );
    }

    @Override
    public int hashCode()
    {
        final String handlerName = getHandlerName();

        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( handlerName == null ) ? 0 : handlerName.hashCode() );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }

        final ContainerDescriptorHandlerConfig other = (ContainerDescriptorHandlerConfig) obj;
        final String handlerName = getHandlerName();
        final String otherHandlerName = other.getHandlerName();

        if ( handlerName == null )
        {
            if ( otherHandlerName != null )
            {
                return false;
            }
        }
        else if ( !handlerName.equals( otherHandlerName ) )
        {
            return false;
        }
        return true;
    }

}
