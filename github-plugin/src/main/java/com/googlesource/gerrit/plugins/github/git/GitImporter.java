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

import com.google.gerrit.server.IdentifiedUser;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.googlesource.gerrit.plugins.github.oauth.HttpSessionProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitImporter extends BatchImporter {

  @Singleton
  public static class Provider extends HttpSessionProvider<GitImporter> {}

  private static final Logger log = LoggerFactory.getLogger(GitImporter.class);
  private final GitCloneStep.Factory cloneFactory;
  private final CreateProjectStep.Factory projectFactory;
  private final ReplicateProjectStep.Factory replicateFactory;


  @Inject
  public GitImporter(GitCloneStep.Factory cloneFactory,
      CreateProjectStep.Factory projectFactory,
      ReplicateProjectStep.Factory replicateFactory, JobExecutor executor,
      IdentifiedUser user) {
    super(executor, user);
    this.cloneFactory = cloneFactory;
    this.projectFactory = projectFactory;
    this.replicateFactory = replicateFactory;
  }

  public void clone(int idx, String organisation, String repository,
      String description) {
    try {
      GitCloneStep cloneStep = cloneFactory.create(organisation, repository);
      CreateProjectStep projectStep =
          projectFactory.create(organisation, repository, description,
              user.getUserName());
      ReplicateProjectStep replicateStep =
          replicateFactory.create(organisation, repository);
      GitImportJob gitCloneJob =
          new GitImportJob(idx, organisation, repository, cloneStep,
              projectStep, replicateStep);
      log.debug("New Git clone job created: " + gitCloneJob);
      schedule(idx, gitCloneJob);
    } catch (Throwable e) {
      schedule(idx, new ErrorJob(idx, organisation, repository, e));
    }
  }
}
