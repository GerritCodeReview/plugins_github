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
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.common.data.AccessSection;
import com.google.gerrit.common.data.GroupDescription;
import com.google.gerrit.common.data.GroupReference;
import com.google.gerrit.common.data.Permission;
import com.google.gerrit.common.data.PermissionRule;
import com.google.gerrit.reviewdb.client.AccountGroup;
import com.google.gerrit.reviewdb.client.AccountGroup.UUID;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.reviewdb.client.Project.InheritableBoolean;
import com.google.gerrit.reviewdb.client.Project.NameKey;
import com.google.gerrit.reviewdb.client.Project.SubmitType;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.GroupBackend;
import com.google.gerrit.server.git.MetaDataUpdate;
import com.google.gerrit.server.git.MetaDataUpdate.User;
import com.google.gerrit.server.git.ProjectConfig;
import com.google.gerrit.server.project.ProjectCache;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

public class GitCloneStep extends ImportStep {
  private static final Logger log = LoggerFactory.getLogger(GitImporter.class);

  private final File gitDir;
  private File destinationDirectory;

  public interface Factory {
    GitCloneStep create(@Assisted("organisation") String organisation,
        @Assisted("name") String repository);
  }

  @Inject
  public GitCloneStep(GitConfig gitConfig,
      MetaDataUpdate.User metaDataUpdateFactory, 
      GroupBackend groupBackend,
      ProjectCache projectCache,
      @Assisted("organisation") String organisation,
      @Assisted("name") String repository)
      throws GitDestinationAlreadyExistsException,
      GitDestinationNotWritableException {
    super(organisation, repository);
    
    this.gitDir = gitConfig.gitDir;
    this.destinationDirectory =
        getDestinationDirectory(organisation, repository);
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

  @Override
  public void doImport(ProgressMonitor progress) throws GitCloneFailedException,
      GitDestinationAlreadyExistsException, GitDestinationNotWritableException {
    CloneCommand clone = new CloneCommand();
    String sourceUri = getSourceUri();
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

  private boolean isNotEmpty(File destDirectory) {
    return destDirectory.listFiles().length > 0;
  }

  public File getDestinationDirectory() {
    return destinationDirectory;
  }

  public boolean rollback() {
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
