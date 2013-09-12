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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gerrit.server.IdentifiedUser;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.github.oauth.GitHubLogin;
import com.googlesrouce.gerrit.plugins.github.git.GitJob;
import com.googlesrouce.gerrit.plugins.github.git.GitImporter;

@Singleton
public class RepositoriesCloneStatusController implements VelocityController {
  private Provider<GitImporter> clonerProvider;

  @Inject
  public RepositoriesCloneStatusController(Provider<GitImporter> clonerProvider) {
    this.clonerProvider = clonerProvider;
  }

  @Override
  public void doAction(IdentifiedUser user, GitHubLogin hubLogin,
      HttpServletRequest req, HttpServletResponse resp, ControllerErrors errors)
      throws ServletException, IOException {
    GitImporter cloner = clonerProvider.get();

    JsonArray reposStatus = new JsonArray();
    for (GitJob job : cloner.getCloneJobs()) {
      reposStatus.add(getJsonStatus(job));

    }
    resp.getWriter().println(reposStatus.toString());
  }

  private JsonElement getJsonStatus(GitJob job) {
    JsonObject json = new JsonObject();
    json.add("index", new JsonPrimitive(job.getIndex()));
    json.add("organisation", new JsonPrimitive(job.getOrganisation()));
    json.add("repository", new JsonPrimitive(job.getRepository()));
    json.add("status", new JsonPrimitive(job.getStatus().toString()));
    json.add("value", new JsonPrimitive(job.getStatusDescription()));
    return json;
  }
}
