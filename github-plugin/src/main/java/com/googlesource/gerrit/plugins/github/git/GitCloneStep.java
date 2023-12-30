// Copyright (C) 2013 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.googlesource.gerrit.plugins.github.git;

import static java.util.stream.Collectors.toList;

import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.api.changes.NotifyHandling;
import com.google.gerrit.extensions.api.projects.ProjectInput;
import com.google.gerrit.extensions.events.ProjectDeletedListener;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.util.ManualRequestContext;
import com.google.gerrit.server.util.OneOffRequestContext;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.googlesource.gerrit.plugins.github.GitHubConfig;
import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitCloneStep extends ImportStep {
  private static final Logger LOG = LoggerFactory.getLogger(GitImporter.class);

  private final GitHubConfig config;
  private final GerritApi gerritApi;
  private final OneOffRequestContext context;
  private final File destinationDirectory;
  private final DynamicSet<ProjectDeletedListener> deletedListeners;
  private final ProjectCache projectCache;
  private final GitRepositoryManager repoManager;
  private final String projectName;

  public interface Factory {
    GitCloneStep create(
        @Assisted("organisation") String organisation, @Assisted("name") String repository);
  }

  @Inject
  public GitCloneStep(
      GitHubConfig config,
      GitHubRepository.Factory gitHubRepoFactory,
      GerritApi gerritApi,
      OneOffRequestContext context,
      DynamicSet<ProjectDeletedListener> deletedListeners,
      ProjectCache projectCache,
      GitRepositoryManager repoManager,
      @Assisted("organisation") String organisation,
      @Assisted("name") String repository)
      throws GitException {
    super(config.gitHubUrl, organisation, repository, gitHubRepoFactory);
    LOG.debug("GitHub Clone " + organisation + "/" + repository);
    this.config = config;

    this.gerritApi = gerritApi;
    this.context = context;
    this.projectName = organisation + "/" + repository;
    this.destinationDirectory = prepareTargetGitDirectory(config.gitDir.toFile(), this.projectName);
    this.deletedListeners = deletedListeners;
    this.projectCache = projectCache;
    this.repoManager = repoManager;
  }

  private static File prepareTargetGitDirectory(File gitDir, String projectName)
      throws GitException {
    File repositoryDir = new File(gitDir, projectName + ".git");
    if (repositoryDir.exists()) {
      throw new GitDestinationAlreadyExistsException(projectName);
    }
    return repositoryDir;
  }

  private void createNewProject() throws GitException {
    try (ManualRequestContext requestContext = context.openAs(config.importAccountId)) {
      ProjectInput pi = new ProjectInput();
      pi.name = projectName;
      GitHubRepository ghRepository = getRepository();
      pi.parent = config.getBaseProject(ghRepository.isPrivate());
      pi.branches = Stream.ofNullable(ghRepository.getDefaultBranch()).collect(toList());
      gerritApi.projects().create(pi).get();
    } catch (ResourceConflictException e) {
      throw new GitDestinationAlreadyExistsException(projectName);
    } catch (RestApiException e) {
      throw new GitException("Unable to create repository " + projectName, e);
    }
  }

  @Override
  public void doImport(ProgressMonitor progress) throws GitException {
    createNewProject();
    String sourceUri = getSourceUri();
    try (Git git = Git.open(destinationDirectory)) {
      FetchCommand fetch =
          git.fetch().setRefSpecs("^refs/changes/*", "refs/*:refs/*").setRemote(sourceUri);
      fetch.setCredentialsProvider(getRepository().getCredentialsProvider());
      if (progress != null) {
        fetch.setProgressMonitor(progress);
      }
      LOG.info(sourceUri + "| Clone into " + destinationDirectory);
      fetch.call();
    } catch (IOException | GitAPIException e) {
      LOG.error("Unable to fetch from {} into {}", sourceUri, destinationDirectory, e);
      throw new GitCloneFailedException(sourceUri, e);
    }
  }

  @Override
  public boolean rollback() {
    File gitDirectory = destinationDirectory;
    if (!gitDirectory.exists()) {
      return false;
    }

    try {
      Project.NameKey key = Project.nameKey(projectName);
      cleanJGitCache(key);
      FileUtils.deleteDirectory(gitDirectory);
      projectCache.remove(key);
      sendProjectDeletedEvent(projectName);
      return true;
    } catch (IOException e) {
      LOG.error("Cannot clean-up output Git directory " + gitDirectory);
      return false;
    }
  }

  private void cleanJGitCache(Project.NameKey key) throws IOException {
    try (Repository repository = repoManager.openRepository(key)) {
      RepositoryCache.close(repository);
    }
  }

  private void sendProjectDeletedEvent(String projectName) {
    ProjectDeletedListener.Event event =
        new ProjectDeletedListener.Event() {
          @Override
          public String getProjectName() {
            return projectName;
          }

          @Override
          public NotifyHandling getNotify() {
            return NotifyHandling.NONE;
          }
        };
    for (ProjectDeletedListener l : deletedListeners) {
      try {
        l.onProjectDeleted(event);
      } catch (RuntimeException e) {
        LOG.warn("Failure in ProjectDeletedListener", e);
      }
    }
  }
}
