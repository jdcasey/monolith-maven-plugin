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

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.commonjava.maven.plugins.monolith.util.DomUtils.getChild;
import static org.commonjava.maven.plugins.monolith.util.DomUtils.getChildrenFromPath;
import static org.commonjava.maven.plugins.monolith.util.DomUtils.writeDocument;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.maven.plugin.assembly.filter.ComponentsXmlArchiverFileFilter;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.ResourceIterator;
import org.codehaus.plexus.components.io.fileselectors.FileInfo;
import org.codehaus.plexus.logging.Logger;
import org.commonjava.maven.plugins.monolith.comp.MonolithVersioningContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Components XML file filter.
 * 
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id: ComponentsXmlArchiverFileFilter.java 999612 2010-09-21 20:34:50Z jdcasey $
 */
public class ComponentsXmlHandler
    extends AbstractMonolithDescriptorHandler
{

    // [jdcasey] Switched visibility to protected to allow testing. Also, because this class isn't final, it should
    // allow
    // some minimal access to the components accumulated for extending classes.
    private final Map<String, Element> components = new LinkedHashMap<>();

    public static final String COMPONENTS_XML_PATH = "META-INF/plexus/components.xml";

    public ComponentsXmlHandler( final MonolithVersioningContext monolithVersioningContext, final Logger logger )
    {
        super( monolithVersioningContext, logger );
    }

    protected void addComponentsXml( final InputStream stream )
        throws IOException
    {
        Document doc;
        try
        {
            doc = DocumentBuilderFactory.newInstance()
                                        .newDocumentBuilder()
                                        .parse( stream );
        }
        catch ( SAXException | ParserConfigurationException e )
        {
            throw new IOException( "Failed to parse components.xml: " + e.getMessage(), e );
        }

        final List<Element> components = getChildrenFromPath( doc.getDocumentElement(), "components/component" );
        for ( final Element component : components )
        {
            final Element roleEl = getChild( component, "role" );
            final Element hintEl = getChild( component, "role-hint" );
            final Element implEl = getChild( component, "implementation" );

            String key = roleEl.getTextContent();
            if ( hintEl != null )
            {
                key += hintEl.getTextContent();
            }

            logger.info( "Adding component: " + key + " with implementation: " + implEl.getTextContent()
                + " (there are already " + this.components.size() + " components defined)" );

            if ( this.components.containsKey( key ) )
            {
                logger.warn( "Duplicate definitions for component " + key + "!" );
            }
            else
            {
                this.components.put( key, component );
            }
        }
    }

    private void addToArchive( final Archiver archiver )
        throws IOException, ArchiverException
    {
        if ( components != null )
        {
            final File f = File.createTempFile( "maven-assembly-plugin", "tmp" );
            f.deleteOnExit();

            FileOutputStream out = null;
            try
            {
                out = new FileOutputStream( f );
                final Document document = DocumentBuilderFactory.newInstance()
                                                                .newDocumentBuilder()
                                                                .newDocument();
                final Element cset = document.createElement( "component-set" );
                document.appendChild( cset );

                final Element cs = document.createElement( "components" );
                cset.appendChild( cs );

                for ( final Element component : components.values() )
                {
                    final Element c = (Element) document.importNode( component, true );
                    cs.appendChild( c );
                }

                writeDocument( document, out );
            }
            catch ( TransformerException | ParserConfigurationException e )
            {
                throw new IOException( "Failed to construct/write aggregated components.xml document: "
                    + e.getMessage(), e );
            }
            finally
            {
                closeQuietly( out );
            }

            archiver.addFile( f, COMPONENTS_XML_PATH );
        }
    }

    @Override
    protected void generateOnCreation( final Archiver archiver )
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

        try
        {
            addToArchive( archiver );
        }
        catch ( final IOException e )
        {
            throw new ArchiverException( "Error finalizing component-set for archive. Reason: " + e.getMessage(), e );
        }
    }

    @Override
    protected List<String> getGeneratedFileList()
    {
        if ( !components.isEmpty() )
        {
            return Collections.singletonList( COMPONENTS_XML_PATH );
        }

        return null;
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

            if ( ComponentsXmlArchiverFileFilter.COMPONENTS_XML_PATH.equals( entry ) )
            {
                InputStream stream = null;
                try
                {
                    stream = fileInfo.getContents();
                    addComponentsXml( stream );
                }
                finally
                {
                    closeQuietly( stream );
                }

                return true;
            }
        }

        return false;
    }

    @Override
    public void clearState()
    {
        components.clear();
        logger.info( "components map cleared; new size is: " + components.size() );
    }

}
