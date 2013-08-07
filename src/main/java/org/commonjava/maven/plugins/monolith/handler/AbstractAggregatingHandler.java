package org.commonjava.maven.plugins.monolith.handler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.assembly.utils.AssemblyFileUtils;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.ResourceIterator;
import org.codehaus.plexus.components.io.fileselectors.FileInfo;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.IOUtil;
import org.commonjava.maven.plugins.monolith.comp.MonolithVersioningContext;

public abstract class AbstractAggregatingHandler
    extends AbstractMonolithDescriptorHandler
{

    private final Map<String, List<String>> catalog = new HashMap<String, List<String>>();

    protected abstract String getOutputPathPrefix( final FileInfo fileInfo );

    protected abstract boolean fileMatches( final FileInfo fileInfo );

    protected AbstractAggregatingHandler( final MonolithVersioningContext context, final Logger logger )
    {
        super( context, logger );
    }

    protected String getEncoding()
    {
        return "UTF-8";
    }

    @Override
    public void generateOnCreation( final Archiver archiver )
        throws ArchiverException
    {
        // this will prompt the isSelected() call, below, for all resources added to the archive.
        // FIXME: This needs to be corrected in the AbstractArchiver, where
        // runArchiveFinalizers() is called before regular resources are added...
        // which is done because the manifest needs to be added first, and the
        // manifest-creation component is a finalizer in the assembly plugin...
        for ( final ResourceIterator it = archiver.getResources(); it.hasNext(); )
        {
            it.next();
        }

        addToArchive( archiver );
    }

    protected void addToArchive( final Archiver archiver )
        throws ArchiverException
    {
        final Map<String, List<String>> catalog = getCatalog();

        for ( final Map.Entry<String, List<String>> entry : catalog.entrySet() )
        {
            final String name = entry.getKey();
            final String fname = new File( name ).getName();

            PrintWriter writer = null;
            File f;
            try
            {
                f = File.createTempFile( "assembly-" + fname, ".tmp" );
                f.deleteOnExit();

                writer = new PrintWriter( new OutputStreamWriter( new FileOutputStream( f ), getEncoding() ) );
                for ( final String line : entry.getValue() )
                {
                    writer.println( line );
                }
            }
            catch ( final IOException e )
            {
                throw new ArchiverException( "Error adding aggregated content for: " + fname
                    + " to finalize archive creation. Reason: " + e.getMessage(), e );
            }
            finally
            {
                IOUtil.close( writer );
            }

            archiver.addFile( f, name );
        }
    }

    @Override
    public List<String> getGeneratedFileList()
    {
        return new ArrayList<String>( catalog.keySet() );
    }

    @Override
    public boolean process( final FileInfo fileInfo )
        throws IOException
    {
        String name = fileInfo.getName();
        name = AssemblyFileUtils.normalizePath( name );
        name = name.replace( File.separatorChar, '/' );

        if ( fileInfo.isFile() && fileMatches( fileInfo ) )
        {
            name = getOutputPathPrefix( fileInfo ) + new File( name ).getName();

            final Map<String, List<String>> catalog = getCatalog();
            List<String> lines = catalog.get( name );
            if ( lines == null )
            {
                lines = new ArrayList<String>();
                catalog.put( name, lines );
            }

            readLines( fileInfo, lines );

            return true;
        }

        return false;
    }

    protected void readLines( final FileInfo fileInfo, final List<String> lines )
        throws IOException
    {
        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader( new InputStreamReader( fileInfo.getContents(), getEncoding() ) );

            String line = null;
            while ( ( line = reader.readLine() ) != null )
            {
                if ( !lines.contains( line ) )
                {
                    lines.add( line );
                }
            }
        }
        finally
        {
            IOUtil.close( reader );
        }
    }

    protected final Map<String, List<String>> getCatalog()
    {
        return catalog;
    }

    protected final void setCatalog( final Map<String, List<String>> catalog )
    {
        this.catalog.clear();
        this.catalog.putAll( catalog );
    }

    @Override
    public void clearState()
    {
        catalog.clear();
    }
}
