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

package com.googlesource.gerrit.plugins.github.oauth;

import java.io.IOException;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.github.oauth.OAuthProtocol.AccessToken;
import com.googlesource.gerrit.plugins.github.oauth.OAuthProtocol.Scope;

public class GitHubLogin {
  private static final Logger LOG = LoggerFactory.getLogger(GitHubLogin.class);

  @Singleton
  public static class Provider extends HttpSessionProvider<GitHubLogin> {
    @Inject
    public Provider(com.google.inject.Provider<GitHubLogin> provider) {
      super(provider);
    }
  }

  public AccessToken token;
  public GitHub hub;

  private transient OAuthProtocol oauth;

  private GHMyself myself;
  private final OAuthCookieProvider cookieProvider;

  public GHMyself getMyself() {
    if (isLoggedIn()) {
      return myself;
    } else {
      return null;
    }
  }

  @Inject
  public GitHubLogin(OAuthProtocol oauth) {
    this.oauth = oauth;
    this.cookieProvider = new OAuthCookieProvider(TokenCipher.get());
  }

  public boolean isLoggedIn() {
    boolean loggedIn = token != null && hub != null;
    if (loggedIn && myself == null) {
      try {
        myself = hub.getMyself();
      } catch (Throwable e) {
        LOG.error("Connection to GitHub broken: logging out", e);
        logout();
        loggedIn = false;
      }
    }
    return loggedIn;
  }

  public boolean login(ServletRequest request, ServletResponse response,
      Scope... scopes) throws IOException {
    return login((HttpServletRequest) request, (HttpServletResponse) response,
        scopes);
  }

  public boolean login(HttpServletRequest request,
      HttpServletResponse response, Scope... scopes) throws IOException {
    if (isLoggedIn()) {
      return true;
    }

    LOG.debug("Login " + this);

    if (OAuthProtocol.isOAuthFinal(request)) {
      LOG.debug("Login-FINAL " + this);
      login(oauth.loginPhase2(request, response));
      if (isLoggedIn()) {
        LOG.debug("Login-SUCCESS " + this);
        String user = myself.getLogin();
        String email = myself.getEmail();
        String fullName =
            Strings.emptyToNull(myself.getName()) == null ? user : myself
                .getName();

        OAuthCookie userCookie =
            cookieProvider.getFromUser(user, email, fullName);
        response.addCookie(userCookie);

        response.sendRedirect(OAuthProtocol.getTargetUrl(request));
        return true;
      } else {
        response.sendError(HttpStatus.SC_UNAUTHORIZED);
        return false;
      }
    } else {
      LOG.debug("Login-PHASE1 " + this);
      oauth.loginPhase1(request, response, scopes);
      return false;
    }
  }

  public void logout() {
    hub = null;
    token = null;
  }

  public OAuthProtocol getOAuthProtocol() {
    return oauth;
  }

  public GitHub login(AccessToken authToken) throws IOException {
    this.token = authToken;
    this.hub = GitHub.connectUsingOAuth(authToken.access_token);
    return this.hub;
  }

  @Override
  public String toString() {
    return "GitHubLogin [token=" + token + ", myself=" + myself + "]";
  }

}
