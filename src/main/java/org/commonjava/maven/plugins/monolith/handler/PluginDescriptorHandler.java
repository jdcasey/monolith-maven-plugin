/*
 * Copyright (c) 2011 Red Hat, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see 
 * <http://www.gnu.org/licenses>.
 */

package org.commonjava.maven.plugins.monolith.handler;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.commonjava.maven.plugins.monolith.util.DomUtils.getChild;
import static org.commonjava.maven.plugins.monolith.util.DomUtils.getChildren;
import static org.commonjava.maven.plugins.monolith.util.DomUtils.writeDocument;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.components.io.fileselectors.FileInfo;
import org.codehaus.plexus.logging.Logger;
import org.commonjava.maven.plugins.monolith.comp.MonolithVersioningContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class PluginDescriptorHandler
    extends AbstractMonolithDescriptorHandler
{

    private static final String PLUGIN_XML_PATH = "META-INF/maven/plugin.xml";

    private Document doc;

    public PluginDescriptorHandler( final MonolithVersioningContext context, final Logger logger )
    {
        super( context, logger );
    }

    @Override
    protected boolean process( final FileInfo fileInfo )
        throws IOException
    {
        if ( fileInfo.isFile() )
        {
            String entry = fileInfo.getName()
                                   .replace( '\\', '/' );

            if ( entry.startsWith( "/" ) )
            {
                entry = entry.substring( 1 );
            }

            if ( PLUGIN_XML_PATH.equals( entry ) )
            {
                logger.info( "Found plugin descriptor..." );
                if ( doc == null )
                {
                    logger.info( "Parsing plugin descriptor." );

                    try
                    {
                        doc = DocumentBuilderFactory.newInstance()
                                                    .newDocumentBuilder()
                                                    .parse( fileInfo.getContents() );
                    }
                    catch ( SAXException | ParserConfigurationException e )
                    {
                        throw new ArchiverException( "Failed to parse plugin.xml file: " + e.getMessage(), e );
                    }

                    final String modifiedVersion = monolithVersioningContext.getCurrentMonolithVersion();
                    if ( modifiedVersion != null )
                    {
                        final Element version = getChild( doc.getDocumentElement(), "version" );
                        if ( version != null && !version.getTextContent()
                                                        .equals( modifiedVersion ) )
                        {
                            version.setTextContent( modifiedVersion );
                        }
                    }

                    final Element depRoot = getChild( doc.getDocumentElement(), "dependencies" );
                    if ( depRoot != null )
                    {
                        final List<Element> deps = getChildren( depRoot, "dependency" );
                        for ( final Element dep : deps )
                        {
                            if ( !monolithVersioningContext.isMonolith( dep ) )
                            {
                                depRoot.removeChild( dep );
                            }
                        }
                    }
                }
                else
                {
                    logger.info( "Skipping...plugin descriptor was already stored for inclusion." );
                }

                return true;
            }
        }

        return false;
    }

    @Override
    protected void generateOnCreation( final Archiver archiver )
        throws ArchiverException
    {
        if ( doc != null )
        {
            File f = null;
            FileOutputStream stream = null;
            try
            {
                f = File.createTempFile( "maven-assembly-plugin", "tmp" );
                f.deleteOnExit();

                stream = new FileOutputStream( f );
                writeDocument( doc, stream );
            }
            catch ( TransformerException | IOException e )
            {
                throw new ArchiverException( "Failed to write modified plugin.xml descriptor: " + e.getMessage(), e );
            }
            finally
            {
                closeQuietly( stream );
            }

            logger.info( "ADDING - Plugin descriptor: " + PLUGIN_XML_PATH + " from file: " + f.getAbsolutePath() );
            archiver.addFile( f, PLUGIN_XML_PATH );
        }
        else if ( logger.isDebugEnabled() )
        {
            logger.debug( "SKIPPING - Plugin descriptor was not encountered. NOT ADDING." );
        }
    }

    @Override
    protected List<String> getGeneratedFileList()
    {
        logger.info( "Parsed plugin.xml: " + doc );
        return doc == null ? null : Collections.singletonList( PLUGIN_XML_PATH );
    }

    @Override
    public void clearState()
    {
        logger.info( "CLEARING parsed plugin.xml: " + doc );
        doc = null;
    }

}
