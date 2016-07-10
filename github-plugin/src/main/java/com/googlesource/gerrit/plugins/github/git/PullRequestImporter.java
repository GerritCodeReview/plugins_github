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
import com.google.inject.servlet.SessionScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SessionScoped
public class PullRequestImporter extends BatchImporter {
  private static final Logger log = LoggerFactory.getLogger(PullRequestImporter.class);
  
  private final PullRequestImportJob.Factory prImportJobProvider;
  
  @Inject
  public PullRequestImporter(JobExecutor executor, IdentifiedUser user,
      PullRequestImportJob.Factory  prImportJobProvider) {
    super(executor, user);
    this.prImportJobProvider = prImportJobProvider;
  }

  public void importPullRequest(int idx, String organisation, String repoName,
      int pullRequestId, PullRequestImportType importType) {
    try {
      PullRequestImportJob pullRequestImportJob = prImportJobProvider.create(idx, organisation, repoName, pullRequestId, importType);
      log.debug("New Pull request import job created: " + pullRequestImportJob);
      schedule(idx, pullRequestImportJob);
    } catch (Throwable e) {
      schedule(idx, new ErrorJob(idx, organisation, repoName, e));
    }
    
  }
}
