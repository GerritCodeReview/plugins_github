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
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRepository;

import com.google.common.base.Strings;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.github.oauth.GitHubLogin;

@Singleton
public class RepositoriesListController implements VelocityController {

  private ProjectCache projects;

  @Inject
  public RepositoriesListController(ProjectCache projects) {
    this.projects = projects;
  }

  @Override
  public void doAction(IdentifiedUser user, GitHubLogin hubLogin,
      HttpServletRequest req, HttpServletResponse resp, ControllerErrors errors)
      throws ServletException, IOException {
    String organisation = req.getParameter("organisation");
    JsonArray jsonRepos = new JsonArray();

    List<GHRepository> myRepositories = getRepositories(hubLogin, organisation);
    for (GHRepository hubRepository : myRepositories) {
      JsonObject repository = new JsonObject();
      String projectName = organisation + "/" + hubRepository.getName();
      if (projects.get(Project.NameKey.parse(projectName)) == null) {
        repository.add("name", new JsonPrimitive(hubRepository.getName()));
        repository.add("organisation", new JsonPrimitive(organisation));
        repository.add("description",
            new JsonPrimitive(Strings.nullToEmpty(hubRepository.getDescription())));
        jsonRepos.add(repository);
      }
    }

    resp.getWriter().println(jsonRepos.toString());
  }

  private List<GHRepository> getRepositories(GitHubLogin hubLogin,
      String organisation) throws IOException {
    if (organisation.equals(hubLogin.getMyself().getLogin())) {
      return hubLogin.getMyself().listRepositories().asList();
    } else {
      GHOrganization ghOrganisation =
          hubLogin.getMyself().getOrganizations().byLogin(organisation);
      return ghOrganisation.listRepositories().asList();
    }
  }
}
