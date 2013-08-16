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
package com.googlesrouce.gerrit.plugins.github.git;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

public class GitClone {
  private static final String GITHUB_REPOSITORY_FORMAT =
      "https://github.com/%1$s/%2$s.git";
  private static final Logger log = LoggerFactory.getLogger(GitCloner.class);
  private final String organisation;
  private final String repository;

  private final File gitDir;
  private String sourceUri;
  private File destinationDirectory;

  public interface Factory {
    GitClone create(@Assisted("organisation") String organisation,
        @Assisted("name") String repository);
  }

  @Inject
  public GitClone(GitConfig gitConfig,
      @Assisted("organisation") String organisation,
      @Assisted("name") String repository)
      throws GitDestinationAlreadyExistsException,
      GitDestinationNotWritableException {
    this.gitDir = gitConfig.gitDir;
    this.organisation = organisation;
    this.repository = repository;
    this.sourceUri = getSourceUri(organisation, repository);
    this.destinationDirectory =
        getDestinationDirectory(organisation, repository);
  }

  public void doClone(ProgressMonitor progress) throws GitCloneFailedException,
      GitDestinationAlreadyExistsException, GitDestinationNotWritableException {
    CloneCommand clone = new CloneCommand();
    clone.setURI(sourceUri);
    clone.setBare(true);
    clone.setDirectory(destinationDirectory);
    if (progress != null) {
      clone.setProgressMonitor(progress);
    }
    try {
      log.info(sourceUri + "| Clone into " + destinationDirectory);
      clone.call();
    } catch (Throwable e) {
      throw new GitCloneFailedException(sourceUri, e);
    }
  }

  private File getDestinationDirectory(String organisation, String repository)
      throws GitDestinationAlreadyExistsException,
      GitDestinationNotWritableException {
    File orgDirectory = new File(gitDir, organisation);
    File destDirectory = new File(orgDirectory, repository + ".git");
    if (destDirectory.exists() && isNotEmpty(destDirectory)) {
      throw new GitDestinationAlreadyExistsException(destDirectory);
    }

    if (!orgDirectory.exists()) {
      if (!orgDirectory.mkdirs()) {
        throw new GitDestinationNotWritableException(destDirectory);
      }
    }

    return destDirectory;
  }

  private boolean isNotEmpty(File destDirectory) {
    return destDirectory.listFiles().length > 0;
  }

  private String getSourceUri(String organisation, String repository) {
    return String.format(GITHUB_REPOSITORY_FORMAT, organisation, repository);
  }

  public String getOrganisation() {
    return organisation;
  }

  public String getRepository() {
    return repository;
  }

  public String getSourceUri() {
    return sourceUri;
  }

  public File getDestinationDirectory() {
    return destinationDirectory;
  }

  public boolean cleanUp() {
    File gitDirectory = destinationDirectory;
    if(!gitDirectory.exists()) {
      return false;
    }
    
    try {
      FileUtils.deleteDirectory(gitDirectory);
      return true;
    } catch (IOException e) {
      log.error("Cannot clean-up output Git directory " + gitDirectory);
      return false;
    }
  }
}
