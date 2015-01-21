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

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class BatchImporter {

  private final ConcurrentHashMap<Integer, GitJob> jobs = new ConcurrentHashMap<Integer, GitJob>();
  private final JobExecutor executor;
  protected final IdentifiedUser user;

  public BatchImporter(final JobExecutor executor, final IdentifiedUser user) {
    this.executor = executor;
    this.user = user;
  }

  public Collection<GitJob> getJobs() {
    return jobs.values();
  }

  public void reset() {
    cancel();
    jobs.clear();
  }

  public void cancel() {
    for (GitJob job : jobs.values()) {
      job.cancel();
    }
  }

  public synchronized void schedule(int idx, GitJob pullRequestImportJob) {
    jobs.put(idx, pullRequestImportJob);
    executor.exec(pullRequestImportJob);
  }

}
