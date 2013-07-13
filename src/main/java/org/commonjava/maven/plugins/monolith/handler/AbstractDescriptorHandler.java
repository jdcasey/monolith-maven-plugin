package org.commonjava.maven.plugins.monolith.handler;

import java.io.IOException;
import java.util.List;

import org.apache.maven.plugin.assembly.filter.ContainerDescriptorHandler;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.components.io.fileselectors.FileInfo;
import org.codehaus.plexus.logging.Logger;

public abstract class AbstractDescriptorHandler
    implements ContainerDescriptorHandler, ResettableDescriptorHandler
{
    @Requirement
    protected Logger logger;

    protected boolean excludeOverride;

    @Override
    public final void finalizeArchiveCreation( final Archiver archiver )
        throws ArchiverException
    {
        try
        {
            generateOnCreation( archiver );
        }
        finally
        {
            clearState();
        }
    }

    @Override
    public void clearState()
    {
        logger.info( "Clearing all state for: " + this );
        excludeOverride = false;
    }

    protected abstract void generateOnCreation( Archiver archiver )
        throws ArchiverException;

    @Override
    public final void finalizeArchiveExtraction( final UnArchiver unarchiver )
        throws ArchiverException
    {
        try
        {
            generateOnExtraction( unarchiver );
        }
        finally
        {
            clearState();
        }
    }

    protected void generateOnExtraction( final UnArchiver unarchiver )
        throws ArchiverException
    {
    }

    @SuppressWarnings( "rawtypes" )
    @Override
    public final List getVirtualFiles()
    {
        return getGeneratedFileList();
    }

    protected abstract List<String> getGeneratedFileList();

    @Override
    public final boolean isSelected( final FileInfo fileInfo )
        throws IOException
    {
        if ( excludeOverride )
        {
            return true;
        }

        if ( process( fileInfo ) )
        {
            return false;
        }

        return true;
    }

    protected abstract boolean process( FileInfo fileInfo )
        throws IOException;

}
