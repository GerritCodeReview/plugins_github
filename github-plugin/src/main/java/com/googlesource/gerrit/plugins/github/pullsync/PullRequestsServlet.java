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
package com.googlesource.gerrit.plugins.github.pullsync;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeInstance;
import org.kohsuke.github.GHCommitPointer;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.reviewdb.client.Project.NameKey;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.project.ProjectState;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.googlesource.gerrit.plugins.github.oauth.GitHubLogin;
import com.googlesource.gerrit.plugins.github.replication.GitHubDestinations;

@Singleton
public class PullRequestsServlet extends HttpServlet {
  private static final long serialVersionUID = 3635343057427548273L;
  private static final Logger log = LoggerFactory
      .getLogger(PullRequestsServlet.class);
  private Provider<GitHubLogin> loginProvider;
  private GitHubDestinations destinations;
  private ProjectCache projects;
  private RuntimeInstance velocityRuntime;

  @Inject
  public PullRequestsServlet(Provider<GitHubLogin> loginProvider,
      GitHubDestinations destinations, ProjectCache projects,
      @Named("PluginRuntimeInstance") RuntimeInstance velocityRuntime) {
    this.loginProvider = loginProvider;
    this.destinations = destinations;
    this.projects = projects;
    this.velocityRuntime = velocityRuntime;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    PrintWriter out = null;
    try {
      GitHub hub = loginProvider.get().hub;
      out = resp.getWriter();

      for (String orgName : destinations.getOrganisations()) {
        GHOrganization org = hub.getOrganization(orgName);
        for (GHRepository repo : org.getRepositories().values()) {
          String repoName = repo.getName();
          ProjectState project = projects.get(new NameKey(repoName));
          if (project == null) {
            log.debug("GitHub repo " + orgName + "/" + repoName
                + " does not have a correspondant Gerrit Project: skipped");
            continue;
          }

          List<GHPullRequest> pullRequests =
              repo.getPullRequests(GHIssueState.OPEN);
          for (GHPullRequest pullRequest : pullRequests) {

            GHCommitPointer pullHead = pullRequest.getHead();
            GHCommitPointer pullBase = pullRequest.getBase();
          }

        }
        org.getRepositories();
      };
    } catch (ResourceNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (ParseErrorException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } finally {
      if (out != null) {
        out.close();
      }
    }
  }
}
