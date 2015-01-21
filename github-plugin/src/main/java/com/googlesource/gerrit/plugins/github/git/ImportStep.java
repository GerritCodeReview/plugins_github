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

import com.googlesource.gerrit.plugins.github.GitHubURL;

import org.eclipse.jgit.lib.ProgressMonitor;

public abstract class ImportStep {
  private final GitHubRepository gitHubRepository;

  public ImportStep(@GitHubURL String gitHubUrl, String organisation,
      String repository, GitHubRepository.Factory ghRepoFactory) {
    this.gitHubRepository =
        ghRepoFactory.create(organisation, repository);
  }

  protected String getSourceUri() {
    return gitHubRepository.getCloneUrl();
  }

  public String getOrganisation() {
    return gitHubRepository.getOrganisation();
  }

  public String getRepositoryName() {
    return gitHubRepository.getRepository();
  }

  public GitHubRepository getRepository() {
    return gitHubRepository;
  }

  public abstract void doImport(ProgressMonitor progress) throws Exception;

  public abstract boolean rollback();
}
