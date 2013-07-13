package org.commonjava.maven.plugins.monolith.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.DefaultArtifactFilterManager;
import org.apache.maven.graph.common.ref.ProjectRef;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.plugin.assembly.model.ContainerDescriptorHandlerConfig;
import org.apache.maven.plugin.assembly.model.DependencySet;
import org.apache.maven.plugin.assembly.model.UnpackOptions;
import org.commonjava.maven.plugins.monolith.handler.ComponentsXmlHandler;
import org.commonjava.maven.plugins.monolith.handler.JavaServicesHandler;
import org.commonjava.maven.plugins.monolith.handler.PluginDescriptorHandler;
import org.commonjava.maven.plugins.monolith.handler.SpringDefsHandler;

import edu.emory.mathcs.backport.java.util.Collections;

@SuppressWarnings( "unchecked" )
public class AssemblyDescriptorBuilder
    extends DefaultArtifactFilterManager // This is a total hack, but the least-bad way to get the core exclusion set.
{

    private static final Set<String> DEFAULT_DEP_EXCLUDES;

    private static final Set<String> DEFAULT_UNPACK_EXCLUDES;

    private static final Set<UniqueContainerDescriptorHandlerConfig> DEFAULT_DESCRIPTOR_HANDLERS;

    static
    {
        final Set<String> defUnpackExcludes = new HashSet<String>();
        defUnpackExcludes.add( "**/*.SF" );
        defUnpackExcludes.add( "**/*.RSA" );

        DEFAULT_UNPACK_EXCLUDES = Collections.unmodifiableSet( defUnpackExcludes );

        final Set<String> defDepExcludes = new HashSet<String>();
        defDepExcludes.add( "org.codehaus.plexus:plexus-utils" );
        //        defDepExcludes.add( "org.eclipse.aether:aether-api" );
        //        defDepExcludes.add( "org.eclipse.aether:aether-spi" );
        //        defDepExcludes.add( "org.eclipse.aether:aether-impl" );

        DEFAULT_DEP_EXCLUDES = Collections.unmodifiableSet( defDepExcludes );

        final Set<UniqueContainerDescriptorHandlerConfig> descHandlers =
            new HashSet<UniqueContainerDescriptorHandlerConfig>();

        descHandlers.add( new UniqueContainerDescriptorHandlerConfig( ComponentsXmlHandler.ID ) );
        descHandlers.add( new UniqueContainerDescriptorHandlerConfig( PluginDescriptorHandler.ID ) );
        descHandlers.add( new UniqueContainerDescriptorHandlerConfig( JavaServicesHandler.ID ) );
        descHandlers.add( new UniqueContainerDescriptorHandlerConfig( SpringDefsHandler.ID ) );

        DEFAULT_DESCRIPTOR_HANDLERS = descHandlers;
    }

    private final Set<String> dependencyExcludes = new HashSet<String>( DEFAULT_DEP_EXCLUDES );

    private final Set<String> unpackExcludes = new HashSet<String>( DEFAULT_UNPACK_EXCLUDES );

    private final Set<UniqueContainerDescriptorHandlerConfig> containerDescriptorHandlers =
        new HashSet<UniqueContainerDescriptorHandlerConfig>( DEFAULT_DESCRIPTOR_HANDLERS );

    public AssemblyDescriptorBuilder()
    {
        addMavenCoreExcludes();
    }

    public AssemblyDescriptorBuilder clearDependencyExcludes()
    {
        dependencyExcludes.clear();
        return this;
    }

    public AssemblyDescriptorBuilder clearUnpackExcludes()
    {
        unpackExcludes.clear();
        return this;
    }

    public AssemblyDescriptorBuilder clearContainerDescriptorHandlers()
    {
        containerDescriptorHandlers.clear();
        return this;
    }

    public AssemblyDescriptorBuilder disableMavenCoreExcludes()
    {
        // Remove the excludes for the core of maven.
        dependencyExcludes.removeAll( this.excludedArtifacts );
        return this;
    }

    public AssemblyDescriptorBuilder disableDependencyExclude( final String pattern )
    {
        dependencyExcludes.remove( pattern );
        return this;
    }

    public AssemblyDescriptorBuilder withDependencyExclude( final String pattern )
    {
        dependencyExcludes.add( pattern );
        return this;
    }

    public AssemblyDescriptorBuilder withUnpackExclude( final String pattern )
    {
        unpackExcludes.add( pattern );
        return this;
    }

    public AssemblyDescriptorBuilder withContainerDescriptorHandler( final String name )
    {
        containerDescriptorHandlers.add( new UniqueContainerDescriptorHandlerConfig( name ) );
        return this;
    }

    public AssemblyDescriptorBuilder withContainerDescriptorHandler( final ContainerDescriptorHandlerConfig config )
    {
        containerDescriptorHandlers.add( new UniqueContainerDescriptorHandlerConfig( config ) );
        return this;
    }

    public Assembly build()
    {
        final Assembly assembly = new Assembly();
        assembly.setId( "monolith" );
        assembly.setIncludeBaseDirectory( false );
        assembly.setIncludeSiteDirectory( false );
        assembly.setModelEncoding( "UTF-8" );

        assembly.setContainerDescriptorHandlers( new ArrayList<ContainerDescriptorHandlerConfig>(
                                                                                                  containerDescriptorHandlers ) );

        final DependencySet ds = new DependencySet();
        final List<String> dex = new ArrayList<String>( dependencyExcludes );
        Collections.sort( dex );
        ds.setExcludes( dex );
        ds.setOutputDirectory( "/" );
        ds.setOutputFileNameMapping( null );
        ds.setUseProjectArtifact( true );
        ds.setUnpack( true );

        if ( !unpackExcludes.isEmpty() )
        {
            final UnpackOptions uo = new UnpackOptions();
            uo.setExcludes( new ArrayList<String>( unpackExcludes ) );
            ds.setUnpackOptions( uo );
        }

        assembly.addDependencySet( ds );

        return assembly;
    }

    public AssemblyDescriptorBuilder withDependencyExcludes( final Set<ProjectRef> refs )
    {
        for ( final ProjectRef ref : refs )
        {
            withDependencyExclude( ref.asProjectRef()
                                      .toString() );
        }

        return this;
    }

    public AssemblyDescriptorBuilder addMavenCoreExcludes()
    {
        // Add in the excludes from the core of maven.
        dependencyExcludes.addAll( this.excludedArtifacts );
        return this;
    }

}
