package org.commonjava.maven.plugins.monolith.comp;

import static org.commonjava.maven.plugins.monolith.util.DomUtils.getChildText;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.maven.graph.common.ref.ArtifactRef;
import org.apache.maven.graph.common.ref.ProjectRef;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.commonjava.maven.plugins.monolith.handler.AbstractMonolithDescriptorHandler;
import org.commonjava.maven.plugins.monolith.handler.ComponentsXmlHandler;
import org.commonjava.maven.plugins.monolith.handler.JavaServicesHandler;
import org.commonjava.maven.plugins.monolith.handler.PluginDescriptorHandler;
import org.commonjava.maven.plugins.monolith.handler.SpringDefsHandler;
import org.w3c.dom.Element;

@Component( role = MonolithVersioningContext.class )
public class MonolithVersioningContext
{

    @Requirement
    private Logger logger;

    private Map<ProjectRef, String> monolithVersions;

    private ArtifactRef myRef;

    private HashSet<AbstractMonolithDescriptorHandler> descriptorHandlers;

    public synchronized Set<AbstractMonolithDescriptorHandler> getDescriptorHandlers()
    {
        if ( descriptorHandlers == null )
        {
            descriptorHandlers = new HashSet<>( 4 );

            descriptorHandlers.add( new PluginDescriptorHandler( this, logger ) );
            descriptorHandlers.add( new ComponentsXmlHandler( this, logger ) );
            descriptorHandlers.add( new JavaServicesHandler( this, logger ) );
            descriptorHandlers.add( new SpringDefsHandler( this, logger ) );
        }

        return descriptorHandlers;
    }

    public void setMonolithVersions( final Map<ArtifactRef, String> monolithVersions )
    {
        final Map<ProjectRef, String> mv = new HashMap<>();
        for ( final Entry<ArtifactRef, String> entry : monolithVersions.entrySet() )
        {
            final ArtifactRef key = entry.getKey();
            final String value = entry.getValue();

            mv.put( key.asProjectRef(), value );
        }

        this.monolithVersions = mv;
    }

    public Map<ProjectRef, String> getMonolithVersions()
    {
        return monolithVersions;
    }

    public void clear()
    {
        myRef = null;
        descriptorHandlers = null;
    }

    public ArtifactRef getCurrentMonolith()
    {
        return myRef;
    }

    public void setCurrentMonolith( final ArtifactRef ref )
    {
        myRef = ref;
    }

    public String getCurrentMonolithVersion()
    {
        return monolithVersions.get( getCurrentMonolith().asProjectRef() );
    }

    public boolean isMonolith( final Element dep )
    {
        final String groupId = getChildText( dep, "groupId" );
        final String artifactId = getChildText( dep, "artifactId" );
        final ProjectRef pr = new ProjectRef( groupId, artifactId );
        return monolithVersions.containsKey( pr );
    }

}
