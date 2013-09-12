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

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.server.IdentifiedUser;
import com.google.inject.Inject;
import com.google.inject.servlet.SessionScoped;

@SessionScoped
public class GitImporter {
  private static final Logger log = LoggerFactory.getLogger(GitImporter.class);
  private final GitCloneStep.Factory cloneFactory;
  private final ConcurrentHashMap<Integer, GitJob> cloneJobs =
      new ConcurrentHashMap<Integer, GitJob>();
  private final GitCommandsExecutor executor;
  private IdentifiedUser user;
  private CreateProjectStep.Factory projectFactory;


  @Inject
  public GitImporter(GitCloneStep.Factory cloneFactory,
      CreateProjectStep.Factory projectFactory,
      GitCommandsExecutor executor, IdentifiedUser user) {
    this.cloneFactory = cloneFactory;
    this.projectFactory = projectFactory;
    this.executor = executor;
    this.user = user;
  }

  public void clone(int idx, String organisation, String repository,
      String description) {
    try {
      GitImportJob gitCloneJob =
          new GitImportJob(idx, organisation, repository, cloneFactory.create(
              organisation, repository), projectFactory.create(organisation,
              repository, description, user.getUserName()));
      log.debug("New Git clone job created: " + gitCloneJob);
      executor.exec(gitCloneJob);
      cloneJobs.put(idx, gitCloneJob);
    } catch (Throwable e) {
      cloneJobs.put(idx, new ErrorCloneJob(idx, organisation, repository, e));
    }
  }

  public Collection<GitJob> getCloneJobs() {
    return cloneJobs.values();
  }

  public void reset() {
    cancel();
    cloneJobs.clear();
  }

  public void cancel() {
    for (GitJob job : cloneJobs.values()) {
      job.cancel();
    }
  }
}
