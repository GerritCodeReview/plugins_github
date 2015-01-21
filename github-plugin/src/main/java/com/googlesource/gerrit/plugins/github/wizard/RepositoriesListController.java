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

import com.google.common.base.Strings;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.googlesource.gerrit.plugins.github.GitHubConfig;
import com.googlesource.gerrit.plugins.github.oauth.GitHubLogin;

import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.PagedIterable;
import org.kohsuke.github.PagedIterator;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Singleton
public class RepositoriesListController implements VelocityController {
  private final ProjectCache projects;
  private final GitHubConfig config;


  @Inject
  public RepositoriesListController(final ProjectCache projects,
      final GitHubConfig config) {
    this.projects = projects;
    this.config = config;
  }

  @Override
  public void doAction(IdentifiedUser user, GitHubLogin hubLogin,
      HttpServletRequest req, HttpServletResponse resp, ControllerErrors errors)
      throws ServletException, IOException {
    String organisation = req.getParameter("organisation");

    JsonArray jsonRepos = new JsonArray();
    int numRepos = 0;
    PagedIterator<GHRepository> repoIter =
        getRepositories(hubLogin, organisation).iterator();

    while (repoIter.hasNext() && numRepos < config.repositoryListLimit) {
      GHRepository ghRepository = repoIter.next();
      JsonObject repository = new JsonObject();
      String projectName = organisation + "/" + ghRepository.getName();
      if (projects.get(Project.NameKey.parse(projectName)) == null) {
        repository.add("name", new JsonPrimitive(ghRepository.getName()));
        repository.add("organisation", new JsonPrimitive(organisation));
        repository.add(
            "description",
            new JsonPrimitive(
                Strings.nullToEmpty(ghRepository.getDescription())));
        repository.add("private", new JsonPrimitive(ghRepository.isPrivate()));
        jsonRepos.add(repository);
        numRepos++;
      }
    }

    resp.getWriter().println(jsonRepos.toString());
  }

  private PagedIterable<GHRepository> getRepositories(GitHubLogin hubLogin,
      String organisation) throws IOException {
    if (organisation.equals(hubLogin.getMyself().getLogin())) {
      return hubLogin.getMyself().listRepositories(config.repositoryListPageSize);
    } else {
      GHOrganization ghOrganisation =
          hubLogin.getMyself().getOrganizations().byLogin(organisation);
      return ghOrganisation.listRepositories(config.repositoryListPageSize);
    }
  }
}
