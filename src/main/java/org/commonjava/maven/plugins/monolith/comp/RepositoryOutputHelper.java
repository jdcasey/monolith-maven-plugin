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

package org.commonjava.maven.plugins.monolith.comp;

import java.io.File;
import java.util.Arrays;

import org.apache.maven.graph.common.ref.ArtifactRef;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.commonjava.maven.plugins.monolith.config.AssemblyConfiguration;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.deployment.DeployRequest;
import org.sonatype.aether.deployment.DeploymentException;
import org.sonatype.aether.impl.internal.SimpleLocalRepositoryManager;
import org.sonatype.aether.installation.InstallRequest;
import org.sonatype.aether.installation.InstallationException;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.util.DefaultRepositorySystemSession;
import org.sonatype.aether.util.artifact.DefaultArtifact;

@Component( role = RepositoryOutputHelper.class )
public class RepositoryOutputHelper
{

    @Requirement
    private RepositorySystem repositorySystem;

    public void install( final AssemblyConfiguration config, final RepositorySystemSession rss,
                         final File artifactFile, final ArtifactRef ref )
        throws InstallationException
    {
        final InstallRequest req = new InstallRequest();

        Artifact artifact =
            new DefaultArtifact( ref.getGroupId(), ref.getArtifactId(), ref.getClassifier(), ref.getType(),
                                 ref.getVersionString() );

        artifact = artifact.setFile( artifactFile );

        req.setArtifacts( Arrays.asList( artifact ) );

        final DefaultRepositorySystemSession sess = new DefaultRepositorySystemSession( rss );

        sess.setLocalRepositoryManager( new SimpleLocalRepositoryManager( config.getInstallDirectory() ) );

        repositorySystem.install( sess, req );
    }

    public void deploy( final AssemblyConfiguration config, final RepositorySystemSession rss, final File artifactFile,
                        final ArtifactRef ref )
        throws DeploymentException
    {
        final DeployRequest req = new DeployRequest();
        Artifact artifact =
            new DefaultArtifact( ref.getGroupId(), ref.getArtifactId(), ref.getClassifier(), ref.getType(),
                                 ref.getVersionString() );

        artifact = artifact.setFile( artifactFile );

        req.setArtifacts( Arrays.asList( artifact ) );

        final File repoDir = config.getDeployDirectory();
        req.setRepository( new RemoteRepository( "deployment", "default", repoDir.toURI()
                                                                                 .toString() ) );

        repositorySystem.deploy( rss, req );
    }

}
