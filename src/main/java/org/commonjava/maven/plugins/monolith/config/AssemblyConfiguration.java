package org.commonjava.maven.plugins.monolith.config;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.commonjava.maven.plugins.monolith.comp.MonolithComponentProvider;

public class AssemblyConfiguration
    implements AssemblerConfigurationSource
{

    private MavenArchiveConfiguration archiveConfiguration;

    private File archiveDirectory;

    private String archiverConfig;

    private final MonolithComponentProvider componentProvider;

    private boolean dryRun;

    private File outputDirectory;

    private File tempDirectory;

    private File workingDirectory;

    private String finalName;

    private final MavenProject project;

    private File deployDirectory;

    private File installDirectory;

    private final String monolithVersion;

    public AssemblyConfiguration withFinalName( final String finalName )
    {
        this.finalName = finalName;
        return this;
    }

    public AssemblyConfiguration( final MonolithComponentProvider componentProvider, final MavenProject project,
                                  final String monolithVersion )
    {
        this.componentProvider = componentProvider;
        this.project = project;
        this.monolithVersion = monolithVersion;
        configureDefaults();
    }

    private void configureDefaults()
    {
        final File basedir = project.getBasedir();
        this.finalName =
            project.getGroupId() + "-" + project.getArtifactId() + "-" + monolithVersion.replace( '-', '_' );

        final File targetDir = new File( basedir, "target" );
        this.installDirectory = new File( targetDir, "installed" );
        this.deployDirectory = new File( targetDir, "deployed" );
        this.outputDirectory = new File( targetDir, "monoliths/" + finalName );
        this.archiveDirectory = new File( targetDir, "archives/" + finalName );
        this.tempDirectory = new File( targetDir, "temp/" + finalName );
        this.workingDirectory = new File( targetDir, "temp/" + finalName );
    }

    public File getInstallDirectory()
    {
        return installDirectory;
    }

    public File getDeployDirectory()
    {
        return deployDirectory;
    }

    @Override
    public File getArchiveBaseDirectory()
    {
        return archiveDirectory;
    }

    @Override
    public String getArchiverConfig()
    {
        return archiverConfig;
    }

    @Override
    public File getBasedir()
    {
        return getProject().getBasedir();
    }

    @Override
    public String getClassifier()
    {
        return null;
    }

    @Override
    public String getDescriptor()
    {
        return null;
    }

    @Override
    public String getDescriptorId()
    {
        return null;
    }

    @Override
    public String[] getDescriptorReferences()
    {
        return new String[] { "monolith" };
    }

    @Override
    public String[] getDescriptors()
    {
        return null;
    }

    @Override
    public File getDescriptorSourceDirectory()
    {
        return null;
    }

    @Override
    public String getEncoding()
    {
        return "UTF-8";
    }

    @Override
    public String getEscapeString()
    {
        return "\\";
    }

    @Override
    public List<String> getFilters()
    {
        return null;
    }

    @Override
    public String getFinalName()
    {
        return finalName;
    }

    @Override
    public MavenArchiveConfiguration getJarArchiveConfiguration()
    {
        return archiveConfiguration;
    }

    @Override
    public ArtifactRepository getLocalRepository()
    {
        return getMavenSession().getLocalRepository();
    }

    @Override
    public MavenFileFilter getMavenFileFilter()
    {
        return componentProvider.getMavenFileFilter();
    }

    @Override
    public MavenSession getMavenSession()
    {
        return componentProvider.getSession();
    }

    @Override
    public File getOutputDirectory()
    {
        return outputDirectory;
    }

    @Override
    public MavenProject getProject()
    {
        return project;
    }

    @Override
    public List<MavenProject> getReactorProjects()
    {
        return Arrays.asList( project );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public List<ArtifactRepository> getRemoteRepositories()
    {
        return getProject().getRemoteArtifactRepositories();
    }

    @Override
    public File getSiteDirectory()
    {
        return null;
    }

    @Override
    public String getTarLongFileMode()
    {
        return "gnu";
    }

    @Override
    public File getTemporaryRootDirectory()
    {
        return tempDirectory;
    }

    @Override
    public File getWorkingDirectory()
    {
        return workingDirectory;
    }

    @Override
    public boolean isAssemblyIdAppended()
    {
        return false;
    }

    @Override
    public boolean isDryRun()
    {
        return dryRun;
    }

    @Override
    public boolean isIgnoreDirFormatExtensions()
    {
        return false;
    }

    @Override
    public boolean isIgnoreMissingDescriptor()
    {
        return false;
    }

    @Override
    public boolean isIgnorePermissions()
    {
        return true;
    }

    @Override
    public boolean isSiteIncluded()
    {
        return false;
    }

    @Override
    public boolean isUpdateOnly()
    {
        return false;
    }

    @Override
    public boolean isUseJvmChmod()
    {
        return true;
    }

    public AssemblyConfiguration withArchiveConfiguration( final MavenArchiveConfiguration archiveConfiguration )
    {
        this.archiveConfiguration = archiveConfiguration;
        return this;
    }

    public AssemblyConfiguration withArchiveDirectory( final File archiveDirectory )
    {
        this.archiveDirectory = archiveDirectory;
        return this;
    }

    public AssemblyConfiguration withArchiverConfig( final String archiverConfig )
    {
        this.archiverConfig = archiverConfig;
        return this;
    }

    public AssemblyConfiguration withDryRun( final boolean dryRun )
    {
        this.dryRun = dryRun;
        return this;
    }

    public AssemblyConfiguration withOutputDirectory( final File outputDirectory )
    {
        this.outputDirectory = outputDirectory;
        return this;
    }

    public AssemblyConfiguration withTempDirectory( final File tempDirectory )
    {
        this.tempDirectory = tempDirectory;
        return this;
    }

    public AssemblyConfiguration withWorkingDirectory( final File workingDirectory )
    {
        this.workingDirectory = workingDirectory;
        return this;
    }

}
