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

import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.googlesource.gerrit.plugins.github.GitHubURL;
import java.io.IOException;

import com.googlesource.gerrit.plugins.replication.api.ReplicationRemotesUpdater;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReplicateProjectStep extends ImportStep {
  private static final Logger LOG = LoggerFactory.getLogger(ReplicateProjectStep.class);
  private final DynamicItem<ReplicationRemotesUpdater> replicationRemotesUpdaterItem;
  private final ReplicationRemoteConfigBuilder remoteConfigBuilder;

  public interface Factory {
    ReplicateProjectStep create(
        @Assisted("organisation") String organisation, @Assisted("name") String repository);
  }

  @Inject
  public ReplicateProjectStep(
      final DynamicItem<ReplicationRemotesUpdater> replicationRemotesUpdaterItem,
      final GitHubRepository.Factory gitHubRepoFactory,
      ReplicationRemoteConfigBuilder remoteConfigBuilder,
      @GitHubURL String gitHubUrl,
      @Assisted("organisation") String organisation,
      @Assisted("name") String repository)
      throws IOException {
    super(gitHubUrl, organisation, repository, gitHubRepoFactory);
    this.remoteConfigBuilder = remoteConfigBuilder;
    LOG.debug("Gerrit ReplicateProject " + organisation + "/" + repository);
    this.replicationRemotesUpdaterItem = replicationRemotesUpdaterItem;
  }

  @Override
  public void doImport(ProgressMonitor progress) throws Exception {
    progress.beginTask("Setting up Gerrit replication", 2);

    String repositoryName = getOrganisation() + "/" + getRepositoryName();
    Config remoteConfig = remoteConfigBuilder.build(repositoryName);
    progress.update(1);

    ReplicationRemotesUpdater updater = replicationRemotesUpdaterItem.get();
    if (updater != null) {
      updater.update(remoteConfig);
    }
    progress.update(1);

    progress.endTask();
  }

  @Override
  public boolean rollback() {
    return false;
  }
}
