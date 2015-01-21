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

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gerrit.reviewdb.client.PatchSet;
import com.google.gerrit.reviewdb.client.Project.NameKey;
import com.google.gerrit.reviewdb.client.RevId;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gwtorm.server.OrmException;
import com.google.gwtorm.server.ResultSet;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import com.googlesource.gerrit.plugins.github.GitHubConfig;
import com.googlesource.gerrit.plugins.github.oauth.GitHubLogin;

import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPerson;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestCommitDetail;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Singleton
public class PullRequestListController implements VelocityController {
  private static final Logger LOG = LoggerFactory
      .getLogger(PullRequestListController.class);
  private static final String DATE_FMT = "yyyy-MM-dd HH:mm z";

  private final GitHubConfig config;
  private final ProjectCache projectsCache;
  private final GitRepositoryManager repoMgr;
  private final Provider<ReviewDb> schema;

  @Inject
  public PullRequestListController(final ProjectCache projectsCache,
      final GitRepositoryManager repoMgr, final Provider<ReviewDb> schema,
      final GitHubConfig config) {
    this.projectsCache = projectsCache;
    this.repoMgr = repoMgr;
    this.schema = schema;
    this.config = config;
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
    if (organisation.equals(hubLogin.getMyself().getLogin())) {
      ghOwner = hubLogin.getMyself();
    } else {
      ghOwner = hubLogin.getHub().getOrganization(organisation);
    }
    return getPullRequests(
        hubLogin,
        ghOwner,
        projectsCache.byName(organisation + "/"
            + Strings.nullToEmpty(repository)));
  }

  private Map<String, List<GHPullRequest>> getPullRequests(GitHubLogin login,
      GHPerson ghOwner, Iterable<NameKey> repos) throws IOException {
    int numPullRequests = 0;
    ReviewDb db = schema.get();
    Map<String, List<GHPullRequest>> allPullRequests = Maps.newHashMap();
    try {
      for (NameKey gerritRepoName : repos) {
        Repository gitRepo = repoMgr.openRepository(gerritRepoName);
        try {
          String ghRepoName = gerritRepoName.get().split("/")[1];
          Optional<GHRepository> githubRepo =
              getGHRepository(login, gerritRepoName);
          if (githubRepo.isPresent()) {
            numPullRequests =
                collectPullRequestsFromGitHubRepository(numPullRequests, db,
                    allPullRequests, gitRepo, ghRepoName, githubRepo);
          }
        } finally {
          gitRepo.close();
        }
      }
      return allPullRequests;
    } finally {
      db.close();
    }
  }

  private int collectPullRequestsFromGitHubRepository(int numPullRequests, ReviewDb db,
      Map<String, List<GHPullRequest>> allPullRequests, Repository gitRepo,
      String ghRepoName, Optional<GHRepository> githubRepo)
      throws IncorrectObjectTypeException, IOException {
    List<GHPullRequest> repoPullRequests = Lists.newArrayList();

    if (numPullRequests < config.pullRequestListLimit) {
      for (GHPullRequest ghPullRequest : githubRepo.get()
          .listPullRequests(GHIssueState.OPEN)) {

        if (isAnyCommitOfPullRequestToBeImported(db, gitRepo,
            ghPullRequest)) {
          repoPullRequests.add(ghPullRequest);
          numPullRequests++;
        }
      }
      if (repoPullRequests.size() > 0) {
        allPullRequests.put(ghRepoName, repoPullRequests);
      }
    } else {
      allPullRequests.put(ghRepoName, null);
    }
    return numPullRequests;
  }

  private Optional<GHRepository> getGHRepository(GitHubLogin login,
      NameKey gerritRepoName) throws IOException {
    try {
      return Optional.of(login.getHub().getRepository(gerritRepoName.get()));
    } catch (FileNotFoundException e) {
      LOG.debug("GitHub repository {} cannot be found", gerritRepoName.get());
      return Optional.absent();
    }
  }

  private boolean isAnyCommitOfPullRequestToBeImported(ReviewDb db,
      Repository gitRepo, GHPullRequest ghPullRequest)
      throws IncorrectObjectTypeException, IOException {
    boolean pullRequestToImport = false;
    RevWalk gitWalk = new RevWalk(gitRepo);
    for (GHPullRequestCommitDetail pullRequestCommit : ghPullRequest
        .listCommits()) {
      ObjectId pullRequestHeadObjectId =
          ObjectId.fromString(pullRequestCommit.getSha());

      try {
        gitWalk.parseCommit(pullRequestHeadObjectId);

        ResultSet<PatchSet> patchSets;
        try {
          patchSets =
              db.patchSets().byRevision(new RevId(pullRequestCommit.getSha()));
        } catch (OrmException e) {
          LOG.error("Error whilst fetching patch-sets from DB associated to commit "
              + pullRequestCommit.getSha());
          return false;
        }
        pullRequestToImport = !patchSets.iterator().hasNext();
        patchSets.close();
      } catch (MissingObjectException e) {
        pullRequestToImport = true;
      }
    }
    return pullRequestToImport;
  }
}
