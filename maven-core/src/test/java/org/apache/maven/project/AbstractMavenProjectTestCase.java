package org.apache.maven.project;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codehaus.plexus.testing.PlexusTest;
import org.codehaus.plexus.PlexusContainer;
import org.eclipse.aether.DefaultRepositoryCache;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.junit.jupiter.api.BeforeEach;

import javax.inject.Inject;

/**
 * @author Jason van Zyl
 */
@PlexusTest
public abstract class AbstractMavenProjectTestCase
{
    protected ProjectBuilder projectBuilder;

    @Inject
    protected RepositorySystem repositorySystem;

    @Inject
    protected PlexusContainer container;

    public PlexusContainer getContainer() {
        return container;
    }

    @BeforeEach
    public void setUp()
        throws Exception
    {
        if ( getContainer().hasComponent( ProjectBuilder.class, "test" ) )
        {
            projectBuilder = getContainer().lookup( ProjectBuilder.class, "test" );
        }
        else
        {
            // default over to the main project builder...
            projectBuilder = getContainer().lookup( ProjectBuilder.class );
        }
    }

    protected ProjectBuilder getProjectBuilder()
    {
        return projectBuilder;
    }

    // ----------------------------------------------------------------------
    // Local repository
    // ----------------------------------------------------------------------

    protected File getLocalRepositoryPath()
        throws FileNotFoundException, URISyntaxException
    {
        File markerFile = getFileForClasspathResource( "local-repo/marker.txt" );

        return markerFile.getAbsoluteFile().getParentFile();
    }

    protected static File getFileForClasspathResource( String resource )
        throws FileNotFoundException
    {
        ClassLoader cloader = Thread.currentThread().getContextClassLoader();

        URL resourceUrl = cloader.getResource( resource );

        if ( resourceUrl == null )
        {
            throw new FileNotFoundException( "Unable to find: " + resource );
        }

        return new File( URI.create( resourceUrl.toString().replaceAll( " ", "%20" ) ) );
    }

    protected ArtifactRepository getLocalRepository()
        throws Exception
    {
        return repositorySystem.createLocalRepository( getLocalRepositoryPath() );
    }

    // ----------------------------------------------------------------------
    // Project building
    // ----------------------------------------------------------------------

    protected MavenProject getProjectWithDependencies( File pom )
        throws Exception
    {
        ProjectBuildingRequest configuration = newBuildingRequest();
        configuration.setRemoteRepositories( Arrays.asList( new ArtifactRepository[] {} ) );
        configuration.setProcessPlugins( false );
        configuration.setResolveDependencies( true );

        try
        {
            return projectBuilder.build( pom, configuration ).getProject();
        }
        catch ( Exception e )
        {
            Throwable cause = e.getCause();
            if ( cause instanceof ModelBuildingException )
            {
                String message = "In: " + pom + "\n\n";
                for ( ModelProblem problem : ( (ModelBuildingException) cause ).getProblems() )
                {
                    message += problem + "\n";
                }
                System.out.println( message );
            }

            throw e;
        }
    }

    protected MavenProject getProject( File pom )
        throws Exception
    {
        ProjectBuildingRequest configuration = newBuildingRequest();

        return projectBuilder.build( pom, configuration ).getProject();
    }

    protected MavenProject getProjectFromRemoteRepository( final File pom )
        throws Exception
    {
        final ArtifactRepository testRepository =
            new MavenArtifactRepository( "test",
                                         "file://" + new File( System.getProperty( "basedir", "" ),
                                                               "src/test/remote-repo" ).toURI().getPath(),
                                         new DefaultRepositoryLayout(),
                                         new ArtifactRepositoryPolicy(),
                                         new ArtifactRepositoryPolicy() );

        final ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setLocalRepository( this.getLocalRepository() );
        configuration.setRemoteRepositories( Arrays.asList( testRepository ) );
        initRepoSession( configuration );

        return projectBuilder.build( pom, configuration ).getProject();
    }

    protected ProjectBuildingRequest newBuildingRequest()
        throws Exception
    {
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setLocalRepository( getLocalRepository() );
        initRepoSession( configuration );
        return configuration;
    }

    protected void initRepoSession( ProjectBuildingRequest request )
    {
        File localRepo = new File( request.getLocalRepository().getBasedir() );
        DefaultRepositorySystemSession repoSession = MavenRepositorySystemUtils.newSession();
        repoSession.setCache( new DefaultRepositoryCache() );
        repoSession.setLocalRepositoryManager( new LegacyLocalRepositoryManager( localRepo ) );
        request.setRepositorySession( repoSession );
    }

}
