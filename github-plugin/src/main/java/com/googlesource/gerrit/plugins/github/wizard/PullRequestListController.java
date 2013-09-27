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
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPerson;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gerrit.reviewdb.client.Project.NameKey;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.github.oauth.GitHubLogin;

@Singleton
public class PullRequestListController implements VelocityController {

  private static final String DATE_FMT = "yyyy-MM-dd HH:mm z";
  private static final int MAX_PULL_REQUESTS = 20;
  private ProjectCache projectsCache;

  @Inject
  public PullRequestListController(ProjectCache projectsCache) {
    this.projectsCache = projectsCache;
  }

  @Override
  public void doAction(IdentifiedUser user, GitHubLogin hubLogin,
      HttpServletRequest req, HttpServletResponse resp, ControllerErrors errors)
      throws ServletException, IOException {
    PrintWriter out = resp.getWriter();

    SimpleDateFormat dateFmt = new SimpleDateFormat(DATE_FMT);
    String organisation = req.getParameter("organisation");
    String repository = req.getParameter("repository");
    Map<String, List<GHPullRequest>> pullRequests =
        getPullRequests(hubLogin, organisation, repository);

    JsonArray reposPullRequests = new JsonArray();
    for (Entry<String, List<GHPullRequest>> repoEntry : pullRequests.entrySet()) {
      JsonObject repoPullRequests = new JsonObject();

      repoPullRequests.add("repository", new JsonPrimitive(repoEntry.getKey()));

      if (repoEntry.getValue() != null) {
        JsonArray prArray = new JsonArray();
        for (GHPullRequest pr : repoEntry.getValue()) {
          JsonObject prObj = new JsonObject();
          prObj.add("id", new JsonPrimitive(pr.getNumber()));
          prObj.add("title", new JsonPrimitive(pr.getTitle()));
          prObj.add("body", new JsonPrimitive(pr.getBody()));
          prObj.add("author", new JsonPrimitive(pr.getUser() == null ? "" : pr
              .getUser().getLogin()));
          prObj.add("status", new JsonPrimitive(pr.getState().name()));
          prObj.add("date",
              new JsonPrimitive(dateFmt.format(pr.getUpdatedAt())));

          prArray.add(prObj);
        }
        repoPullRequests.add("pullrequests", prArray);
      }

      reposPullRequests.add(repoPullRequests);
    }
    out.println(reposPullRequests.toString());
  }

  private Map<String, List<GHPullRequest>> getPullRequests(
      GitHubLogin hubLogin, String organisation, String repository)
      throws IOException {
    GHPerson ghOwner;
    if(organisation.equals(hubLogin.getMyself().getLogin())) {
      ghOwner = hubLogin.getMyself();
    } else {
      ghOwner = hubLogin.hub.getOrganization(organisation);
    }
    return getPullRequests(
        hubLogin,
        ghOwner,
        projectsCache.byName(organisation + "/"
            + Strings.nullToEmpty(repository)));
  }

  private Map<String, List<GHPullRequest>> getPullRequests(GitHubLogin login,
      GHPerson ghOwner,
      Iterable<NameKey> repos) throws IOException {
    int numPullRequests = 0;
    Map<String, List<GHPullRequest>> allPullRequests = Maps.newHashMap();
    for (NameKey gerritRepoName : repos) {
      String ghRepoName = gerritRepoName.get().split("/")[1];
      List<GHPullRequest> repoPullRequests = Lists.newArrayList();

      if (numPullRequests < MAX_PULL_REQUESTS) {
        for (GHPullRequest ghPullRequest : GHRepository
            .listPullRequests(login.hub, ghOwner, ghRepoName,
                GHIssueState.OPEN)) {
          repoPullRequests.add(ghPullRequest);
          numPullRequests++;
        }
        if (repoPullRequests.size() > 0) {
          allPullRequests.put(ghRepoName, repoPullRequests);
        }
      } else {
        allPullRequests.put(ghRepoName, null);
      }
    }
    return allPullRequests;
  }
}
