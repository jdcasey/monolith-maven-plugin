package org.commonjava.maven.plugins.monolith;

import static org.apache.commons.lang.StringUtils.join;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.graph.common.ref.ArtifactRef;
import org.apache.maven.graph.common.ref.ProjectRef;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.archive.AssemblyArchiver;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.plugin.assembly.model.io.xpp3.AssemblyXpp3Writer;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingResult;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.commonjava.maven.plugins.monolith.comp.MonolithComponentProvider;
import org.commonjava.maven.plugins.monolith.comp.MonolithVersioningContext;
import org.commonjava.maven.plugins.monolith.comp.PerLookupAssemblyArchiver;
import org.commonjava.maven.plugins.monolith.comp.PomModifier;
import org.commonjava.maven.plugins.monolith.comp.RepositoryOutputHelper;
import org.commonjava.maven.plugins.monolith.config.AssemblyConfiguration;
import org.commonjava.maven.plugins.monolith.config.AssemblyDescriptorBuilder;
import org.commonjava.maven.plugins.monolith.versions.VersionCalculator;
import org.sonatype.aether.deployment.DeploymentException;
import org.sonatype.aether.installation.InstallationException;

@Mojo( name = "create", requiresProject = true, threadSafe = true )
public class CreateMonolithsGoal
    extends AbstractMojo
    implements MonolithComponentProvider
{

    private static final ProjectRef PLEXUS_UTILS_REF = new ProjectRef( "org.codehaus.plexus", "plexus-utils" );

    @Parameter( property = "monolith.version.suffix", defaultValue = "redhat-1" )
    private String versionSuffix;

    @Parameter( property = "monolith.suffix.increment", defaultValue = "true" )
    private boolean increment;

    @Parameter( defaultValue = "${session}", required = true, readonly = true )
    private MavenSession session;

    @Parameter( defaultValue = "${project}", required = true, readonly = true )
    private MavenProject project;

    @Parameter( defaultValue = "${plugin}", required = true, readonly = true )
    private PluginDescriptor thisPlugin;

    @Parameter( defaultValue = "${mavenBundlers}" )
    private List<String> mavenBundlers;

    @Component
    private MavenFileFilter fileFilter;

    @Component
    private ProjectBuilder projectBuilder;

    @Component
    private MonolithVersioningContext monolithVersioningContext;

    @Component
    private RepositorySystem repoSystem;

    @Component
    private PomModifier pomModifier;

    @Component
    private PlexusContainer container;

    @Component
    private RepositoryOutputHelper outputHelper;

    //    @Component( role = ContainerDescriptorHandler.class )
    //    private List<ContainerDescriptorHandler> descriptorHandlers;

    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        final Set<ProjectRef> mvnBundlers = new HashSet<>();
        if ( mavenBundlers != null )
        {
            for ( final String ga : mavenBundlers )
            {
                mvnBundlers.add( ProjectRef.parse( ga ) );
            }
        }

        final VersionCalculator calc = new VersionCalculator( versionSuffix, increment );
        final Map<ArtifactRef, String> todo = new HashMap<>();
        final Map<ProjectRef, String> monolithVersions = new HashMap<>();

        final DependencyManagement dm = project.getDependencyManagement();
        if ( dm != null )
        {
            for ( final Dependency dep : dm.getDependencies() )
            {
                final ArtifactRef ar =
                    new ArtifactRef( dep.getGroupId(), dep.getArtifactId(), dep.getVersion(), dep.getType(),
                                     dep.getClassifier(), false );

                final String modifiedVersion = calc.calculate( dep.getVersion() );
                todo.put( ar, modifiedVersion );
                monolithVersions.put( ar.asProjectRef(), modifiedVersion );
            }
        }

        final PluginManagement pm = project.getPluginManagement();
        if ( pm != null )
        {
            for ( final Plugin plugin : pm.getPlugins() )
            {
                if ( thisPlugin.getGroupId()
                               .equals( plugin.getGroupId() ) && thisPlugin.getArtifactId()
                                                                           .equals( plugin.getArtifactId() ) )
                {
                    continue;
                }

                final ArtifactRef ar =
                    new ArtifactRef( plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion(), "maven-plugin",
                                     null, false );

                final String modifiedVersion = calc.calculate( plugin.getVersion() );

                todo.put( ar, modifiedVersion );
                monolithVersions.put( ar.asProjectRef(), modifiedVersion );
            }
        }

        final ArtifactHandler pomHandler = new DefaultArtifactHandler( "pom" );

        final Map<ArtifactRef, MavenProject> todoProjects = new HashMap<>();
        final LinkedList<ArtifactRef> forProjects = new LinkedList<>( todo.keySet() );
        final Set<ArtifactRef> seen = new HashSet<>();
        while ( !forProjects.isEmpty() )
        {
            final ArtifactRef ref = forProjects.removeFirst();

            if ( seen.contains( ref ) )
            {
                continue;
            }

            getLog().info( "Reading MavenProject + artifacts for: " + ref );

            final DefaultProjectBuildingRequest request =
                new DefaultProjectBuildingRequest( session.getProjectBuildingRequest() );

            request.setResolveDependencies( true );

            final Artifact artifact =
                new DefaultArtifact( ref.getGroupId(), ref.getArtifactId(),
                                     VersionRange.createFromVersion( ref.getVersionString() ), null, "pom", null,
                                     pomHandler );

            try
            {
                final ProjectBuildingResult result = projectBuilder.build( artifact, request );
                final MavenProject monolithProject = result.getProject();

                @SuppressWarnings( "unchecked" )
                final ArtifactResolutionRequest arr =
                    new ArtifactResolutionRequest().setArtifact( monolithProject.getArtifact() )
                                                   .setRemoteRepositories( monolithProject.getRemoteArtifactRepositories() );

                final ArtifactResolutionResult arresult = repoSystem.resolve( arr );
                final Artifact resultArtifact = arresult.getArtifacts()
                                                        .iterator()
                                                        .next();

                if ( resultArtifact.getFile() != null )
                {
                    project.getArtifact()
                           .setFile( resultArtifact.getFile() );
                }

                todoProjects.put( ref, monolithProject );
                seen.add( ref );

                @SuppressWarnings( "unchecked" )
                final List<Dependency> deps = monolithProject.getDependencies();
                for ( final Dependency dep : deps )
                {
                    if ( "org.codehaus.plexus".equals( dep.getGroupId() )
                        && "plexus-utils".equals( dep.getArtifactId() ) )
                    {
                        final ArtifactRef ar =
                            new ArtifactRef( dep.getGroupId(), dep.getArtifactId(), dep.getVersion(), dep.getType(),
                                             dep.getClassifier(), false );

                        if ( !seen.contains( ar ) )
                        {
                            final String v = calc.calculate( dep.getVersion() );

                            todo.put( ar, v );

                            getLog().info( "Adding plexus-utils: " + ar + " for resolution as a monolith subsystem." );
                            forProjects.addLast( ar );
                        }
                    }
                }
            }
            catch ( final ProjectBuildingException e )
            {
                throw new MojoExecutionException( "Failed to build MavenProject instance for: "
                    + ref.asProjectVersionRef() + ". Error: " + e.getMessage(), e );
            }

        }

        try
        {
            monolithVersioningContext.setMonolithVersions( todo );

            final List<File> files = new ArrayList<>();

            for ( final Entry<ArtifactRef, String> entry : todo.entrySet() )
            {
                final ArtifactRef ref = entry.getKey();
                final String monolithVersion = entry.getValue();

                getLog().info( "\n\n\n\nCreating monolith for: " + ref + "\n\n\n\n" );
                monolithVersioningContext.setCurrentMonolith( ref );

                // TODO: 
                // - determine how modified subsystems (and their included deps) may interact with the plugin assembly!
                // - determine the proper order of operations for this goal
                // Then:
                // - rewrite POMs with version suffix
                // - create assemblies

                try
                {
                    final MavenProject monolithProject = todoProjects.get( ref );
                    getLog().info( "Project: " + monolithProject + " has artifact-file: "
                                       + monolithProject.getArtifact()
                                                        .getFile() );

                    project.setVersion( monolithVersion );
                    project.getModel()
                           .setVersion( monolithVersion );
                    project.getOriginalModel()
                           .setVersion( monolithVersion );

                    final AssemblyConfiguration config =
                        new AssemblyConfiguration( this, monolithProject, monolithVersion );

                    AssemblyDescriptorBuilder builder =
                        new AssemblyDescriptorBuilder().withDependencyExcludes( monolithVersions.keySet() )
                                                       .disableDependencyExclude( ref.asProjectRef()
                                                                                     .toString() );

                    if ( mvnBundlers.contains( ref.asProjectRef() ) )
                    {
                        builder = builder.disableMavenCoreExcludes();
                    }
                    else if ( PLEXUS_UTILS_REF.equals( ref.asProjectRef() ) )
                    {
                        builder = builder.clearDependencyExcludes()
                                         .addMavenCoreExcludes();
                    }

                    final Assembly descriptor = builder.build();

                    if ( getLog().isDebugEnabled() )
                    {
                        final StringWriter sw = new StringWriter();
                        new AssemblyXpp3Writer().write( sw, descriptor );
                        getLog().debug( "Using assembly descriptor:\n\n" + sw.toString() + "\n\n" );
                    }

                    final AssemblyArchiver archiver =
                        (AssemblyArchiver) container.lookup( AssemblyArchiver.class.getName(),
                                                             PerLookupAssemblyArchiver.ID );

                    final File assembly =
                        archiver.createArchive( descriptor, config.getFinalName(), "jar", config, false );

                    if ( assembly != null )
                    {
                        files.add( assembly );
                    }

                    final File pom = pomModifier.writeModifiedPom( monolithProject, config );
                    if ( pom != null )
                    {
                        files.add( pom );
                    }

                    final ArtifactRef adjRef =
                        new ArtifactRef( ref.getGroupId(), ref.getArtifactId(), monolithVersion, ref.getType(),
                                         ref.getClassifier(), ref.isOptional() );

                    getLog().info( "Install:\n  POM: " + pom + "\n  Artifact: " + assembly );
                    final ArtifactRef pomRef = new ArtifactRef( adjRef, "pom", null, false );
                    outputHelper.install( config, session.getRepositorySession(), pom, pomRef );
                    outputHelper.install( config, session.getRepositorySession(), assembly, adjRef );

                    getLog().info( "Deploy:\n  POM: " + pom + "\n  Artifact: " + assembly );
                    outputHelper.deploy( config, session.getRepositorySession(), pom, pomRef );
                    outputHelper.deploy( config, session.getRepositorySession(), assembly, adjRef );
                }
                catch ( ArchiveCreationException | AssemblyFormattingException | InvalidAssemblerConfigurationException e )
                {
                    throw new MojoExecutionException( "Failed to create monolith assembly: "
                        + ref.asProjectVersionRef() + ". Error: " + e.getMessage(), e );
                }
                catch ( final IOException e )
                {
                    throw new MojoExecutionException( "Failed to rewrite POM: " + ref.asProjectVersionRef()
                        + ". Error: " + e.getMessage(), e );
                }
                catch ( final ComponentLookupException e )
                {
                    throw new MojoExecutionException( "Cannot lookup AssemblyArchiver: " + e.getMessage(), e );
                }
                catch ( InstallationException | DeploymentException e )
                {
                    throw new MojoExecutionException( "Failed to install/deploy: " + e.getMessage(), e );
                }
            }

            getLog().info( "Wrote:\n\n " + join( files, "\n  " ) + "\n\n" );
        }
        finally
        {
            monolithVersioningContext.clear();
        }

    }

    @Override
    public MavenSession getSession()
    {
        return session;
    }

    @Override
    public MavenFileFilter getMavenFileFilter()
    {
        return fileFilter;
    }

}
