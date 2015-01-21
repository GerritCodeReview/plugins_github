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
package com.googlesource.gerrit.plugins.github.wizard;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;

import com.googlesource.gerrit.plugins.github.git.BatchImporter;
import com.googlesource.gerrit.plugins.github.git.GitJob;
import com.googlesource.gerrit.plugins.github.git.GitJobStatus;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletResponse;



public class JobStatusController {

  public JobStatusController() {
    super();
  }
  
  protected void respondWithJobStatusJson(HttpServletResponse resp, BatchImporter cloner)
      throws IOException {
    Collection<GitJob> jobs = cloner.getJobs();
    List<GitJobStatus> jobListStatus = Lists.newArrayList();
    for (GitJob job : jobs) {
      jobListStatus.add(job.getStatus());
    }
    new Gson().toJson(jobListStatus, jobListStatus.getClass(), new JsonWriter(
        resp.getWriter()));
  }


}
