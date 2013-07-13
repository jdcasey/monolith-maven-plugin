package org.commonjava.maven.plugins.monolith.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public final class DomUtils
{

    private DomUtils()
    {
    }

    public static List<Element> getChildren( final Element in, final String named )
    {
        final List<Element> children = new ArrayList<>();

        final NodeList list = in.getElementsByTagName( named );
        for ( int i = 0; i < list.getLength(); i++ )
        {
            children.add( (Element) list.item( i ) );
        }

        return children;
    }

    public static List<Element> getChildrenFromPath( final Element in, final String path )
    {
        final List<String> pathElements = new ArrayList<>( Arrays.asList( path.split( "[./]" ) ) );
        List<Element> last = new ArrayList<>();
        last.add( in );
        while ( !pathElements.isEmpty() )
        {
            final String nextName = pathElements.remove( 0 );

            final List<Element> next = new ArrayList<>();
            for ( final Element lastEl : last )
            {
                next.addAll( getChildren( lastEl, nextName ) );
            }

            last = next;
        }

        return last;
    }

    public static Element getChild( final Element in, final String named )
    {
        final NodeList list = in.getElementsByTagName( named );
        if ( list.getLength() > 0 )
        {
            return (Element) list.item( 0 );
        }

        return null;
    }

    public static String getChildText( final Element in, final String named )
    {
        final Element e = getChild( in, named );
        return e == null ? null : e.getTextContent();
    }

    public static void writeDocument( final Document doc, final OutputStream out )
        throws IOException, TransformerException
    {
        final TransformerFactory tf = TransformerFactory.newInstance();
        final Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty( OutputKeys.OMIT_XML_DECLARATION, "no" );
        transformer.setOutputProperty( OutputKeys.METHOD, "xml" );
        transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
        transformer.setOutputProperty( OutputKeys.ENCODING, "UTF-8" );
        transformer.setOutputProperty( "{http://xml.apache.org/xslt}indent-amount", "2" );

        transformer.transform( new DOMSource( doc ), new StreamResult( new OutputStreamWriter( out, "UTF-8" ) ) );
    }
}
