package org.commonjava.maven.plugins.monolith.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.assembly.filter.ContainerDescriptorHandler;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.ResourceIterator;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.components.io.fileselectors.FileInfo;
import org.codehaus.plexus.logging.Logger;
import org.commonjava.maven.plugins.monolith.comp.MonolithVersioningContext;

@Component( role = ContainerDescriptorHandler.class, hint = MonolithDescriptorHandlerManager.ID, instantiationStrategy = "per-lookup" )
public class MonolithDescriptorHandlerManager
    implements ContainerDescriptorHandler
{

    public static final String ID = "monolith";

    @Requirement
    private Logger logger;

    @Requirement
    private MonolithVersioningContext monolithVersioningContext;

    private boolean excludeOverride;

    @Override
    public final void finalizeArchiveCreation( final Archiver archiver )
        throws ArchiverException
    {
        final Set<AbstractMonolithDescriptorHandler> handlers = monolithVersioningContext.getDescriptorHandlers();

        logger.info( "Pre-iterating all resources ONE TIME to trigger ContainerDescriptorHandlers." );

        // this will prompt the isSelected() call, below, for all resources added to the archive.
        // FIXME: This needs to be corrected in the AbstractArchiver, where
        // runArchiveFinalizers() is called before regular resources are added...
        // which is done because the manifest needs to be added first, and the
        // manifest-creation component is a finalizer in the assembly plugin...
        for ( final ResourceIterator it = archiver.getResources(); it.hasNext(); )
        {
            it.next();
        }

        logger.info( "Pre-iteration DONE." );

        for ( final AbstractMonolithDescriptorHandler handler : handlers )
        {
            handler.generateOnCreation( archiver );
        }
    }

    public void clearState()
    {
        logger.info( "Clearing all state for: " + this );
        excludeOverride = false;
    }

    @Override
    public final void finalizeArchiveExtraction( final UnArchiver unarchiver )
        throws ArchiverException
    {
        final Set<AbstractMonolithDescriptorHandler> handlers = monolithVersioningContext.getDescriptorHandlers();
        for ( final AbstractMonolithDescriptorHandler handler : handlers )
        {
            handler.generateOnExtraction( unarchiver );
        }
    }

    @SuppressWarnings( "rawtypes" )
    @Override
    public final List getVirtualFiles()
    {
        final List<String> files = new ArrayList<String>();
        final Set<AbstractMonolithDescriptorHandler> handlers = monolithVersioningContext.getDescriptorHandlers();

        for ( final AbstractMonolithDescriptorHandler handler : handlers )
        {
            final List<String> gf = handler.getGeneratedFileList();
            if ( gf != null )
            {
                files.addAll( gf );
            }
        }

        return files;
    }

    @Override
    public final boolean isSelected( final FileInfo fileInfo )
        throws IOException
    {
        if ( excludeOverride )
        {
            return true;
        }

        final Set<AbstractMonolithDescriptorHandler> handlers = monolithVersioningContext.getDescriptorHandlers();
        for ( final AbstractMonolithDescriptorHandler handler : handlers )
        {
            if ( handler.process( fileInfo ) )
            {
                return false;
            }
        }

        return true;
    }

}
