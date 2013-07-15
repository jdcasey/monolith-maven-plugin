package org.commonjava.maven.plugins.monolith.handler;

import java.io.IOException;
import java.util.List;

import org.apache.maven.plugin.assembly.filter.ContainerDescriptorHandler;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.components.io.fileselectors.FileInfo;

@Component( role = ContainerDescriptorHandler.class, hint = DummyPlexusContainerDescriptorHandler.ID )
public class DummyPlexusContainerDescriptorHandler
    implements ContainerDescriptorHandler
{

    public static final String ID = "plexus";

    @Override
    public void finalizeArchiveCreation( final Archiver archiver )
        throws ArchiverException
    {
    }

    @Override
    public void finalizeArchiveExtraction( final UnArchiver unarchiver )
        throws ArchiverException
    {
    }

    @SuppressWarnings( "rawtypes" )
    @Override
    public List getVirtualFiles()
    {
        return null;
    }

    @Override
    public boolean isSelected( final FileInfo fileInfo )
        throws IOException
    {
        return true;
    }

}
