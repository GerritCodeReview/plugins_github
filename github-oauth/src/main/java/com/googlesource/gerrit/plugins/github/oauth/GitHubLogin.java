package com.googlesource.gerrit.plugins.github.oauth;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.kohsuke.github.GitHub;

import com.google.inject.Inject;
import com.google.inject.servlet.SessionScoped;
import com.googlesource.gerrit.plugins.github.oauth.OAuthProtocol.AccessToken;

@SessionScoped
public class GitHubLogin {
  public AccessToken token;
  public GitHub hub;

  private transient OAuthProtocol oauth;

  @Inject
  public GitHubLogin(OAuthProtocol oauth) {
    this.oauth = oauth;
  }

  public GitHubLogin(GitHub hub, AccessToken token) {
    this.hub = hub;
    this.token = token;
  }

  public boolean isLoggedIn() {
    return token != null && hub != null;
  }

  public boolean login(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    if (oauth.isOAuthFinal(request)) {
      init(oauth.loginPhase2(request, response));
      return isLoggedIn();
    } else {
      oauth.loginPhase1(request, response);
      return false;
    }
  }

  private void init(GitHubLogin initValues) {
    this.hub = initValues.hub;
    this.token = initValues.token;
  }
}
