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

import org.eclipse.jgit.lib.ProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.googlesource.gerrit.plugins.github.GitHubURL;
import com.googlesource.gerrit.plugins.github.oauth.GitHubLogin;

public class ReplicateProjectStep extends ImportStep {
  private static final Logger LOG = LoggerFactory.getLogger(ReplicateProjectStep.class);
  private final ReplicationConfig replicationConfig;
  private final String authUsername;
  private final String authToken;

  public interface Factory {
    ReplicateProjectStep create(@Assisted("organisation") String organisation,
        @Assisted("name") String repository, @Assisted GitHubLogin ghLogin);
  }


  @Inject
  public ReplicateProjectStep(final ReplicationConfig replicationConfig,
      @GitHubURL String gitHubUrl,
      @Assisted("organisation") String organisation,
      @Assisted("name") String repository,
      @Assisted GitHubLogin ghLogin) {
    super(gitHubUrl, organisation, repository);
    LOG.debug("Gerrit ReplicateProject " + organisation + "/" + repository);
    this.replicationConfig = replicationConfig;
    this.authUsername = ghLogin.getMyself().getLogin();
    this.authToken = ghLogin.token.access_token;
  }

  @Override
  public void doImport(ProgressMonitor progress) throws Exception {
    progress.beginTask("Setting up Gerrit replication", 2);

    String repositoryName = getOrganisation() + "/" + getRepository();
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
