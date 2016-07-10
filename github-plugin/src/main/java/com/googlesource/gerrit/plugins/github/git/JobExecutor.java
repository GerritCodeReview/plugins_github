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

import com.google.gerrit.server.util.RequestScopePropagator;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.googlesource.gerrit.plugins.github.GitHubConfig;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Singleton
public class JobExecutor {
  private final ScheduledExecutorService executor;
  private final RequestScopePropagator requestScopePropagator;
  private final GitHubConfig config;

  @Inject
  public JobExecutor(final RequestScopePropagator requestScopePropagator,
      final GitHubConfig config) {
    this.requestScopePropagator = requestScopePropagator;
    this.config = config;
    this.executor = Executors
        .newScheduledThreadPool(config.jobPoolLimit);
  }

  public void exec(GitJob job) {
    executor.schedule(requestScopePropagator.wrap(job),
        getRandomExecutionDelay(job), TimeUnit.SECONDS);
  }

  private int getRandomExecutionDelay(GitJob job) {
    Random rnd = new Random(System.currentTimeMillis() + job.hashCode());
    return rnd.nextInt(config.jobExecTimeout);
  }
}
