package org.apache.maven.lifecycle.internal.builder;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
  <<<<<<< MNG-5563
import java.util.Map;
  =======
import java.util.Optional;
  >>>>>>> master
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.BuildFailure;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.feature.Features;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.LifecycleNotFoundException;
import org.apache.maven.lifecycle.LifecyclePhaseNotFoundException;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.lifecycle.internal.DefaultLifecyclePluginAnalyzer;
import org.apache.maven.lifecycle.internal.ExecutionEventCatapult;
import org.apache.maven.lifecycle.internal.LifecycleDebugLogger;
import org.apache.maven.lifecycle.internal.LifecycleExecutionPlanCalculator;
import org.apache.maven.lifecycle.internal.ReactorContext;
import org.apache.maven.lifecycle.internal.TaskSegment;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoNotFoundException;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.prefix.NoPluginFoundForPrefixException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Common code that is shared by the LifecycleModuleBuilder and the LifeCycleWeaveBuilder
 *
 * @since 3.0
 * @author Kristian Rosenvold
 *         Builds one or more lifecycles for a full module
 *         NOTE: This class is not part of any public api and can be changed or deleted without prior notice.
 */
@Named
@Singleton
public class BuilderCommon
{
    @Inject
    private LifecycleDebugLogger lifecycleDebugLogger;

    @Inject
    private LifecycleExecutionPlanCalculator lifeCycleExecutionPlanCalculator;

    @Inject
    private ExecutionEventCatapult eventCatapult;

    @Inject
    private Logger logger;

    public BuilderCommon()
    {
    }

    public BuilderCommon( LifecycleDebugLogger lifecycleDebugLogger,
                          LifecycleExecutionPlanCalculator lifeCycleExecutionPlanCalculator, Logger logger )
    {
        this.lifecycleDebugLogger = lifecycleDebugLogger;
        this.lifeCycleExecutionPlanCalculator = lifeCycleExecutionPlanCalculator;
        this.logger = logger;
    }

    public MavenExecutionPlan resolveBuildPlan( MavenSession session, MavenProject project, TaskSegment taskSegment,
                                                Set<Artifact> projectArtifacts )
        throws PluginNotFoundException, PluginResolutionException, LifecyclePhaseNotFoundException,
        PluginDescriptorParsingException, MojoNotFoundException, InvalidPluginDescriptorException,
        NoPluginFoundForPrefixException, LifecycleNotFoundException, PluginVersionResolutionException,
        LifecycleExecutionException
    {
        MavenExecutionPlan executionPlan =
            lifeCycleExecutionPlanCalculator.calculateExecutionPlan( session, project, taskSegment.getTasks() );

        lifecycleDebugLogger.debugProjectPlan( project, executionPlan );

  <<<<<<< MNG-5563
        findNonThreadSafePlugins( project, executionPlan, session );

        findUnversionedPlugins( project, executionPlan );

        findUnknownParameterNames( project, executionPlan );

        return executionPlan;
    }

    private void findNonThreadSafePlugins( MavenProject project, MavenExecutionPlan executionPlan,
                                               MavenSession session )
    {
  =======
        // With Maven 4's build/consumer the POM will always rewrite during distribution.
        // The maven-gpg-plugin uses the original POM, causing an invalid signature.
        // Fail as long as there's no solution available yet
        if ( Features.buildConsumer().isActive() )
        {
            Optional<MojoExecution> gpgMojo = executionPlan.getMojoExecutions().stream()
                            .filter( m -> "maven-gpg-plugin".equals( m.getArtifactId() ) 
                                       && "org.apache.maven.plugins".equals( m.getGroupId() ) )
                            .findAny();

            if ( gpgMojo.isPresent() )
            {
                throw new LifecycleExecutionException( "The maven-gpg-plugin is not supported by Maven 4."
                    + " Verify if there is a compatible signing solution,"
                    + " add -D" + Features.buildConsumer().propertyName() + "=false"
                    + " or use Maven 3." );
            }
        }

  >>>>>>> master
        if ( session.getRequest().getDegreeOfConcurrency() > 1 )
        {
            final Set<Plugin> unsafePlugins = executionPlan.getNonThreadSafePlugins();
            if ( !unsafePlugins.isEmpty() )
            {
                logger.warn( "*****************************************************************" );
                logger.warn( "* Your build is requesting parallel execution, but project      *" );
                logger.warn( "* contains the following plugin(s) that have goals not marked   *" );
                logger.warn( "* as @threadSafe to support parallel building.                  *" );
                logger.warn( "* While this /may/ work fine, please look for plugin updates    *" );
                logger.warn( "* and/or request plugins be made thread-safe.                   *" );
                logger.warn( "* If reporting an issue, report it against the plugin in        *" );
                logger.warn( "* question, not against maven-core                              *" );
                logger.warn( "*****************************************************************" );
                if ( logger.isDebugEnabled() )
                {
                    final Set<MojoDescriptor> unsafeGoals = executionPlan.getNonThreadSafeMojos();
                    logger.warn( "The following goals are not marked @threadSafe in " + project.getName() + ":" );
                    for ( MojoDescriptor unsafeGoal : unsafeGoals )
                    {
                        logger.warn( unsafeGoal.getId() );
                    }
                }
                else
                {
                    logger.warn( "The following plugins are not marked @threadSafe in " + project.getName() + ":" );
                    for ( Plugin unsafePlugin : unsafePlugins )
                    {
                        logger.warn( unsafePlugin.getId() );
                    }
                    logger.warn( "Enable debug to see more precisely which goals are not marked @threadSafe." );
                }
                logger.warn( "*****************************************************************" );
            }
        }
  <<<<<<< MNG-5563
    }

    private void findUnversionedPlugins( MavenProject project, MavenExecutionPlan executionPlan )
    {
  =======

  >>>>>>> master
        final String defaulModelId = DefaultLifecyclePluginAnalyzer.DEFAULTLIFECYCLEBINDINGS_MODELID;

        List<String> unversionedPlugins = executionPlan.getMojoExecutions().stream()
                         .map( MojoExecution::getPlugin )
                         .filter( p -> p.getLocation( "version" ) != null ) // versionless cli goal (?)
                         .filter( p -> p.getLocation( "version" ).getSource() != null ) // versionless in pom (?)
                         .filter( p -> defaulModelId.equals( p.getLocation( "version" ).getSource().getModelId() ) )
                         .distinct()
                         .map( Plugin::getArtifactId ) // managed by us, groupId is always o.a.m.plugins
                         .collect( Collectors.toList() );

        if ( !unversionedPlugins.isEmpty() )
        {
            logger.warn( "Version not locked for default bindings plugins " + unversionedPlugins
                + ", you should define versions in pluginManagement section of your " + "pom.xml or parent" );
        }
    }

    private void findUnknownParameterNames( MavenProject project, MavenExecutionPlan executionPlan )
    {
        // Verify that all configured parameters in the pom match a parameter of the mojo
        Map<Plugin, List<MojoExecution>> sharedExecutions = executionPlan.getMojoExecutions().stream()
            .collect( Collectors.groupingBy( MojoExecution::getPlugin ) );
        
        for ( Map.Entry<Plugin, List<MojoExecution>> entry : sharedExecutions.entrySet() )
        {
            Map<String, Set<String>> validParametersPerExecutionId = new HashMap<>();
            
            entry.getValue().stream()
                .filter( e -> e.getMojoDescriptor().getParameters() != null )
                .forEach( e -> validParametersPerExecutionId.computeIfAbsent( e.getExecutionId(), 
                      k -> new HashSet<>() ).addAll( parameterNames( e.getMojoDescriptor().getParameters() ) ) );

            for ( Map.Entry<String, PluginExecution> pc : entry.getKey().getExecutionsAsMap().entrySet() )
            {
                Set<String> validParams =
                    validParametersPerExecutionId.getOrDefault( pc.getKey(), Collections.emptySet() );
                Xpp3Dom pluginConfiguration = (Xpp3Dom) pc.getValue().getConfiguration();
                
                if ( pluginConfiguration != null )
                {
                    List<Xpp3Dom> unknownParameters = Arrays.stream( pluginConfiguration.getChildren() )
                                    .filter( e -> !validParams.contains( e.getName() ) )
                                    .collect( Collectors.toList() );

                    if ( !unknownParameters.isEmpty() )
                    {
                        logger.warn( String.format( "Unknown parameters for %s (%s):", 
                                                   entry.getKey().getId(), pc.getKey(), unknownParameters ) );
                        
                        for ( Xpp3Dom param : unknownParameters )
                        {
                            InputLocation loc = (InputLocation) param.getInputLocation();
                            
                            // in case the parameter is defined in 'project', we could break the build
                            logger.warn( String.format( "  <%s> @ %s, line %s", 
                                        param.getName(), loc.getSource().getModelId(), loc.getLineNumber() ) );
                        }
                    }
                }
            }
        }
    }

    private Set<String> parameterNames( List<Parameter> parameters )
    {
        Set<String> names = new HashSet<>( parameters.size() * 2 );
        for ( Parameter p : parameters )
        {
            names.add( p.getName() );
            names.add( p.getAlias() );
        }
        return names;
    }

    public void handleBuildError( final ReactorContext buildContext, final MavenSession rootSession,
                                  final MavenSession currentSession, final MavenProject mavenProject, Throwable t,
                                  final long buildStartTime )
    {
        // record the error and mark the project as failed
        long buildEndTime = System.currentTimeMillis();
        buildContext.getResult().addException( t );
        buildContext.getResult().addBuildSummary( new BuildFailure( mavenProject, buildEndTime - buildStartTime, t ) );

        // notify listeners about "soft" project build failures only
        if ( t instanceof Exception && !( t instanceof RuntimeException ) )
        {
            eventCatapult.fire( ExecutionEvent.Type.ProjectFailed, currentSession, null, (Exception) t );
        }

        // reactor failure modes
        if ( t instanceof RuntimeException || !( t instanceof Exception ) )
        {
            // fail fast on RuntimeExceptions, Errors and "other" Throwables
            // assume these are system errors and further build is meaningless
            buildContext.getReactorBuildStatus().halt();
        }
        else if ( MavenExecutionRequest.REACTOR_FAIL_NEVER.equals( rootSession.getReactorFailureBehavior() ) )
        {
            // continue the build
        }
        else if ( MavenExecutionRequest.REACTOR_FAIL_AT_END.equals( rootSession.getReactorFailureBehavior() ) )
        {
            // continue the build but ban all projects that depend on the failed one
            buildContext.getReactorBuildStatus().blackList( mavenProject );
        }
        else if ( MavenExecutionRequest.REACTOR_FAIL_FAST.equals( rootSession.getReactorFailureBehavior() ) )
        {
            buildContext.getReactorBuildStatus().halt();
        }
        else
        {
            logger.error( "invalid reactor failure behavior " + rootSession.getReactorFailureBehavior() );
            buildContext.getReactorBuildStatus().halt();
        }
    }

    public static void attachToThread( MavenProject currentProject )
    {
        ClassRealm projectRealm = currentProject.getClassRealm();
        if ( projectRealm != null )
        {
            Thread.currentThread().setContextClassLoader( projectRealm );
        }
    }

    // TODO I'm really wondering where this method belongs; smells like it should be on MavenProject, but for some
    // reason it isn't ? This localization is kind-of a code smell.

    public static String getKey( MavenProject project )
    {
        return project.getGroupId() + ':' + project.getArtifactId() + ':' + project.getVersion();
    }

}
