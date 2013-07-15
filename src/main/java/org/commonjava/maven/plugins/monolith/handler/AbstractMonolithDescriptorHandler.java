package org.commonjava.maven.plugins.monolith.handler;

import java.io.IOException;
import java.util.List;

import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.components.io.fileselectors.FileInfo;
import org.codehaus.plexus.logging.Logger;
import org.commonjava.maven.plugins.monolith.comp.MonolithVersioningContext;

public abstract class AbstractMonolithDescriptorHandler
{

    protected final MonolithVersioningContext monolithVersioningContext;

    protected final Logger logger;

    protected AbstractMonolithDescriptorHandler( final MonolithVersioningContext context, final Logger logger )
    {
        this.monolithVersioningContext = context;
        this.logger = logger;
    }

    abstract void clearState();

    abstract void generateOnCreation( Archiver archiver )
        throws ArchiverException;

    abstract List<String> getGeneratedFileList();

    abstract boolean process( FileInfo fileInfo )
        throws IOException;

    void generateOnExtraction( final UnArchiver unarchiver )
        throws ArchiverException
    {
    }

}
