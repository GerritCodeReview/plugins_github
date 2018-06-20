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

import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.googlesource.gerrit.plugins.github.GitHubConfig;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitCloneStep extends ImportStep {
  private static final Logger LOG = LoggerFactory.getLogger(GitImporter.class);

  private final File gitDir;
  private final GerritApi gerritApi;
  private File destinationDirectory;

  public interface Factory {
    GitCloneStep create(
        @Assisted("organisation") String organisation, @Assisted("name") String repository);
  }

  @Inject
  public GitCloneStep(
      GitHubConfig gitConfig,
      GitHubRepository.Factory gitHubRepoFactory,
      GerritApi gerritApi,
      @Assisted("organisation") String organisation,
      @Assisted("name") String repository)
      throws GitException {
    super(gitConfig.gitHubUrl, organisation, repository, gitHubRepoFactory);
    LOG.debug("GitHub Clone " + organisation + "/" + repository);
    this.gitDir = gitConfig.gitDir.toFile();

    this.gerritApi = gerritApi;
    this.destinationDirectory =
        prepareTargetGitDirectory(gerritApi, gitDir, organisation, repository);
  }

  private static File prepareTargetGitDirectory(
      GerritApi gerritApi, File gitDir, String organisation, String repository)
      throws GitException {
    String projectName = organisation + "/" + repository;
    try {
      gerritApi.projects().create(projectName).get();
      return new File(gitDir, projectName + ".git");
    } catch (ResourceConflictException e) {
      throw new GitDestinationAlreadyExistsException(projectName);
    } catch (RestApiException e) {
      throw new GitException("Unable to create repository " + projectName, e);
    }
  }

  @Override
  public void doImport(ProgressMonitor progress) throws GitCloneFailedException {
    String sourceUri = getSourceUri();
    try (Git git = Git.open(destinationDirectory)) {
      FetchCommand fetch = git.fetch().setRefSpecs("refs/*:refs/*").setRemote(sourceUri);
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

  private boolean isNotEmpty(File destDirectory) {
    return destDirectory.listFiles().length > 0;
  }

  public File prepareTargetGitDirectory() {
    return destinationDirectory;
  }

  @Override
  public boolean rollback() {
    File gitDirectory = destinationDirectory;
    if (!gitDirectory.exists()) {
      return false;
    }

    try {
      FileUtils.deleteDirectory(gitDirectory);
      return true;
    } catch (IOException e) {
      LOG.error("Cannot clean-up output Git directory " + gitDirectory);
      return false;
    }
  }
}
