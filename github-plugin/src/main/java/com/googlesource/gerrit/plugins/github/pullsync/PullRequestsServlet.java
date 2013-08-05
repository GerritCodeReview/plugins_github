package com.googlesource.gerrit.plugins.github.pullsync;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.kohsuke.github.GHCommitPointer;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.reviewdb.client.Project.NameKey;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.project.ProjectControl;
import com.google.gerrit.server.project.ProjectState;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.github.oauth.GitHubLogin;
import com.googlesource.gerrit.plugins.github.replication.Destination;
import com.googlesource.gerrit.plugins.github.replication.GitHubDestinations;

@Singleton
public class PullRequestsServlet extends HttpServlet {
  private static final long serialVersionUID = 3635343057427548273L;
  private static final Logger log = LoggerFactory
      .getLogger(PullRequestsServlet.class);
  private Provider<GitHubLogin> loginProvider;
  private GitHubDestinations destinations;
  private ProjectCache projects;

  @Inject
  public PullRequestsServlet(Provider<GitHubLogin> loginProvider,
      GitHubDestinations destinations, ProjectCache projects) {
    this.loginProvider = loginProvider;
    this.destinations = destinations;
    this.projects = projects;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    PrintWriter out = null;
    try {
      GitHubLogin hubLogin = loginProvider.get();
      if (!hubLogin.isLoggedIn() && !hubLogin.login(req, resp)) {
        return;
      }

      GitHub hub = hubLogin.hub;
      out = resp.getWriter();

      out.println("<html><body>");

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

          out.println("<h1>Project: " + project.getProject().getName()
              + "</h1>");

          List<GHPullRequest> pullRequests =
              repo.getPullRequests(GHIssueState.OPEN);
          for (GHPullRequest pullRequest : pullRequests) {

            out.println("<form id=\"%s\">");
            out.println("<H2>Pull Request #" + pullRequest.getNumber()
                + "</H2>");
            out.println("> Title: " + pullRequest.getTitle());
            out.println("> Body: " + pullRequest.getBody());

            GHCommitPointer pullHead = pullRequest.getHead();
            out.println("> Pull Repository: "
                + pullHead.getRepository().getUrl());
            out.println("> Pull SHA-1: " + pullHead.getSha());
            out.println("> Pull ref-spec: " + pullHead.getRef());

            GHCommitPointer pullBase = pullRequest.getBase();
            out.println("> Base Repository: "
                + pullBase.getRepository().getUrl());
            out.println("> Base SHA-1: " + pullBase.getSha());
            out.println("> Base ref-spec: " + pullBase.getRef());
          }

        }
        org.getRepositories();
      };
      out.println("</body></html>");
    } finally {
      if (out != null) {
        out.close();
      }
    }
  }
}
