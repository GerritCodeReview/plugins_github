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
import java.util.Arrays;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.servlet.SessionScoped;
import com.googlesource.gerrit.plugins.github.oauth.OAuthProtocol.AccessToken;
import com.googlesource.gerrit.plugins.github.oauth.OAuthProtocol.Scope;

@SessionScoped
public class GitHubLogin {
  private static final Logger log = LoggerFactory.getLogger(GitHubLogin.class);

  public AccessToken token;
  public GitHub hub;
  private SortedSet<Scope> scopesSet = new TreeSet<OAuthProtocol.Scope>();

  private transient OAuthProtocol oauth;

  private GHMyself myself;

  public GHMyself getMyself() {
    if (isLoggedIn(scopesSet)) {
      return myself;
    } else {
      return null;
    }
  }

  @Inject
  public GitHubLogin(OAuthProtocol oauth) {
    this.oauth = oauth;
  }

  public GitHubLogin(GitHub hub, AccessToken token, Scope... scopes) {
    this.hub = hub;
    this.token = token;
    this.scopesSet = new TreeSet<OAuthProtocol.Scope>(Arrays.asList(scopes));
  }

  public boolean isLoggedIn(Scope... scopes) {
    return isLoggedIn(new TreeSet<Scope>(Arrays.asList(scopes)));
  }

  public boolean isLoggedIn(Set<Scope> scopes) {
    boolean loggedIn = scopesSet.equals(scopes) && token != null && hub != null;
    if (loggedIn && myself == null) {
      try {
        myself = hub.getMyself();
      } catch (Throwable e) {
        log.error("Connection to GitHub broken: logging out", e);
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
    if (isLoggedIn(scopes)) {
      return true;
    }

    setScopes(scopes);

    if (oauth.isOAuthFinal(request)) {
      init(oauth.loginPhase2(request, response));
      if (isLoggedIn(scopes)) {
        response.sendRedirect(oauth.getTargetUrl(request));
        return true;
      } else {
        response.sendError(HttpStatus.SC_UNAUTHORIZED);
        return false;
      }
    } else {
      oauth.loginPhase1(request, response, scopes);
      return false;
    }
  }

  public void logout() {
    scopesSet = new TreeSet<OAuthProtocol.Scope>();
    hub = null;
    token = null;
  }

  private void setScopes(Scope... scopes) {
    this.scopesSet = new TreeSet<Scope>(Arrays.asList(scopes));
  }

  private void init(GitHubLogin initValues) {
    this.hub = initValues.hub;
    this.token = initValues.token;
  }
}
