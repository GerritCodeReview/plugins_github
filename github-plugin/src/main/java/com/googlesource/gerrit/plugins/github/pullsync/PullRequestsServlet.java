package com.googlesource.gerrit.plugins.github.pullsync;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.github.oauth.GitHubLogin;
import com.googlesource.gerrit.plugins.github.replication.Destination;
import com.googlesource.gerrit.plugins.github.replication.GitHubDestinations;

@Singleton
public class PullRequestsServlet extends HttpServlet {
  private static final long serialVersionUID = 3635343057427548273L;
  private Provider<GitHubLogin> loginProvider;
  private GitHubDestinations destinations;

  @Inject
  public PullRequestsServlet(Provider<GitHubLogin> loginProvider,
      GitHubDestinations destinations) {
    this.loginProvider = loginProvider;
    this.destinations = destinations;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    PrintWriter out = null;
    try {
      GitHubLogin hubLogin = loginProvider.get();
      if (!hubLogin.isLoggedIn()) {
        if (!hubLogin.login(req, resp)) {
          return;
        }
      }
      out = resp.getWriter();

      out.println("<html><body><pre>");
      for (Destination dest : destinations.getDestinations()) {
        out.println(dest.getRemote().getURIs());
      };
      out.println("</pre></body></html>");
    } finally {
      if (out != null) {
        out.close();
      }
    }
  }
}
