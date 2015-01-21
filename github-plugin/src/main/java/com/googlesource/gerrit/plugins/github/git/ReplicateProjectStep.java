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

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import com.googlesource.gerrit.plugins.github.GitHubURL;
import com.googlesource.gerrit.plugins.github.oauth.GitHubLogin;
import com.googlesource.gerrit.plugins.github.oauth.ScopedProvider;

import org.eclipse.jgit.lib.ProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReplicateProjectStep extends ImportStep {
  private static final Logger LOG = LoggerFactory.getLogger(ReplicateProjectStep.class);
  private final ReplicationConfig replicationConfig;
  private final String authUsername;
  private final String authToken;
  private final String gitHubUrl;

  public interface Factory {
    ReplicateProjectStep create(@Assisted("organisation") String organisation,
        @Assisted("name") String repository);
  }


  @Inject
  public ReplicateProjectStep(final ReplicationConfig replicationConfig,
      final GitHubRepository.Factory gitHubRepoFactory,
      final ScopedProvider<GitHubLogin> ghLoginProvider,
      @GitHubURL String gitHubUrl,
      @Assisted("organisation") String organisation,
      @Assisted("name") String repository) {
    super(gitHubUrl, organisation, repository, gitHubRepoFactory);
    LOG.debug("Gerrit ReplicateProject " + organisation + "/" + repository);
    this.replicationConfig = replicationConfig;
    GitHubLogin ghLogin = ghLoginProvider.get();
    this.authUsername = ghLogin.getMyself().getLogin();
    this.authToken = ghLogin.getToken().accessToken;
    this.gitHubUrl = gitHubUrl;
  }

  @Override
  public void doImport(ProgressMonitor progress) throws Exception {
    progress.beginTask("Setting up Gerrit replication", 2);

    String repositoryName = getOrganisation() + "/" + getRepositoryName();
    progress.update(1);
    replicationConfig.addSecureCredentials(getOrganisation(), authUsername,
        authToken);
    progress.update(1);
    replicationConfig.addReplicationRemote(getOrganisation(), gitHubUrl
        + "/${name}.git", repositoryName);
    progress.endTask();
  }

  @Override
  public boolean rollback() {
    return false;
  }

}
