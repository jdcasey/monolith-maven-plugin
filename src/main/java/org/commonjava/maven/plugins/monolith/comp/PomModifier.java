package org.commonjava.maven.plugins.monolith.comp;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.graph.common.ref.ProjectRef;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.WriterFactory;
import org.commonjava.maven.plugins.monolith.config.AssemblyConfiguration;

@Component( role = PomModifier.class )
public class PomModifier
{

    private static final Set<String> MODDABLE_SCOPES;

    static
    {
        final Set<String> moddableScopes = new HashSet<>();
        moddableScopes.add( "runtime" );
        moddableScopes.add( "compile" );

        MODDABLE_SCOPES = moddableScopes;
    }

    @Requirement
    private MonolithVersioningContext monolithVersioningContext;

    public File writeModifiedPom( final MavenProject project, final AssemblyConfiguration config )
        throws IOException
    {
        // We want the effective pom here, to avoid unnecessary dependencies on other poms in the output.
        final Model model = project.getModel();

        final String modifiedVersion = monolithVersioningContext.getCurrentMonolithVersion();
        model.setVersion( modifiedVersion );

        slimPom( model );

        final DependencyManagement dm = model.getDependencyManagement();
        if ( dm != null )
        {
            modifyDeps( dm.getDependencies() );
        }

        modifyDeps( model.getDependencies() );

        final File pom = new File( config.getOutputDirectory(), config.getFinalName() + ".pom" );
        Writer writer = null;
        try
        {
            writer = WriterFactory.newXmlWriter( pom );
            new MavenXpp3Writer().write( writer, model );
        }
        finally
        {
            closeQuietly( writer );
        }

        return pom;
    }

    private void slimPom( final Model model )
    {
        model.setParent( null );
        model.setRepositories( null );
        model.setPluginRepositories( null );
    }

    private void modifyDeps( final List<Dependency> dependencies )
    {
        final Map<ProjectRef, String> monolithVersions = monolithVersioningContext.getMonolithVersions();

        for ( final Dependency dep : dependencies )
        {
            final ProjectRef pr = new ProjectRef( dep.getGroupId(), dep.getArtifactId() );
            final String version = monolithVersions.get( pr );
            if ( version != null )
            {
                dep.setVersion( version );
            }
            else if ( dep.getScope() == null || MODDABLE_SCOPES.contains( dep.getScope() ) )
            {
                dep.setScope( "provided" );
            }
        }
    }

}
