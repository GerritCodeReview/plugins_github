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

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.server.IdentifiedUser;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.github.oauth.HttpSessionProvider;

public class GitImporter extends BatchImporter {

  @Singleton
  public static class Provider extends HttpSessionProvider<GitImporter> {}

  private static final FluentLogger log = FluentLogger.forEnclosingClass();
  private final ProtectedBranchesCheckStep.Factory protectedBranchesCheckFactory;
  private final MagicRefCheckStep.Factory magicRefCheckFactory;
  private final GitCloneStep.Factory cloneFactory;
  private final CreateProjectStep.Factory projectFactory;
  private final ReplicateProjectStep.Factory replicateFactory;

  @Inject
  public GitImporter(
      ProtectedBranchesCheckStep.Factory protectedBranchesCheckFactory,
      GitCloneStep.Factory cloneFactory,
      CreateProjectStep.Factory projectFactory,
      ReplicateProjectStep.Factory replicateFactory,
      MagicRefCheckStep.Factory magicRefCheckFactory,
      JobExecutor executor,
      IdentifiedUser user) {
    super(executor, user);
    this.protectedBranchesCheckFactory = protectedBranchesCheckFactory;
    this.cloneFactory = cloneFactory;
    this.projectFactory = projectFactory;
    this.replicateFactory = replicateFactory;
    this.magicRefCheckFactory = magicRefCheckFactory;
  }

  public void clone(int idx, String organisation, String repository, String description) {
    try {
      ProtectedBranchesCheckStep protectedBranchesCheckStep =
          protectedBranchesCheckFactory.create(organisation, repository);
      GitCloneStep cloneStep = cloneFactory.create(organisation, repository);
      MagicRefCheckStep magicRefCheckStep = magicRefCheckFactory.create(organisation, repository);
      CreateProjectStep projectStep =
          projectFactory.create(organisation, repository, description, user.getUserName().get());
      ReplicateProjectStep replicateStep = replicateFactory.create(organisation, repository);
      GitImportJob gitCloneJob =
          new GitImportJob(
              idx,
              organisation,
              repository,
              protectedBranchesCheckStep,
              magicRefCheckStep,
              cloneStep,
              projectStep,
              replicateStep);
      log.atFine().log("New Git clone job created: " + gitCloneJob);
      schedule(idx, gitCloneJob);
    } catch (Throwable e) {
      schedule(idx, new ErrorJob(idx, organisation, repository, e));
    }
  }
}
