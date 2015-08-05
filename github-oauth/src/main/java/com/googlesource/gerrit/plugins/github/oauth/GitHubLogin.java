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

import static java.util.concurrent.TimeUnit.DAYS;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.Getter;

import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.github.oauth.OAuthProtocol.AccessToken;
import com.googlesource.gerrit.plugins.github.oauth.OAuthProtocol.Scope;

public class GitHubLogin implements Serializable {
  private static final long serialVersionUID = 1L;
  private static final Logger log = LoggerFactory.getLogger(GitHubLogin.class);
  private static final List<Scope> DEFAULT_SCOPES = Arrays.asList(
      Scope.PUBLIC_REPO, Scope.USER_EMAIL, Scope.READ_ORG);
  private static final long SCOPE_COOKIE_NEVER_EXPIRES = DAYS
      .toSeconds(50 * 365);

  @Singleton
  public static class Provider extends HttpSessionProvider<GitHubLogin> {
    @Override
    public GitHubLogin get(HttpServletRequest request) {
      return super.get(request);
    }
  }

  @Getter
  private AccessToken token;

  private String state;

  private SortedSet<Scope> loginScopes;
  private final GitHubOAuthConfig config;

  public GHMyself getMyself() throws IOException {
    if (isLoggedIn()) {
      return getHub().getMyself();
    } else {
      return null;
    }
  }

  public Set<String> getMyOrganisationsLogins() throws IOException {
    if (isLoggedIn()) {
      return getHub().getMyOrganizations().keySet();
    } else {
      return Collections.emptySet();
    }
  }

  @Inject
  public GitHubLogin(final GitHubOAuthConfig config) {
    this.config = config;
  }

  public boolean isLoggedIn() {
    return token != null;
  }

  public boolean login(HttpServletRequest request,
      HttpServletResponse response, OAuthProtocol oauth, Scope... scopes) throws IOException {
    if (isLoggedIn()) {
      return true;
    }

    log.debug("Login " + this);
    if (OAuthProtocol.isOAuthFinal(request)) {
      log.debug("Login-FINAL " + this);
      login(oauth.loginPhase2(request, response, state));
      this.state = ""; // Make sure state is used only once

      if (isLoggedIn()) {
        log.debug("Login-SUCCESS " + this);
        response.sendRedirect(OAuthProtocol.getTargetUrl(request));
        return true;
      } else {
        return false;
      }
    } else {
      this.loginScopes = getScopes(getScopesKey(request, response), scopes);
      log.debug("Login-PHASE1 " + this);
      state = oauth.loginPhase1(request, response, loginScopes);
      return false;
    }
  }

  public void logout() {
    token = null;
  }

  public GitHub login(AccessToken authToken) throws IOException {
    log.debug("Logging in using access token {}", authToken.accessToken);
    this.token = authToken;
    return getHub();
  }

  @Override
  public String toString() {
    return "GitHubLogin [token=" + token + ", scopes="
        + loginScopes + "]";
  }

  public GitHub getHub() throws IOException {
    return GitHub.connectUsingOAuth(this.token.accessToken);
  }

  private String getScopesKey(HttpServletRequest request,
      HttpServletResponse response) {
    String scopeRequested = request.getParameter("scope");
    if (scopeRequested == null) {
      scopeRequested = getScopesKeyFromCookie(request);
    }

    if (scopeRequested != null) {
      Cookie scopeCookie = new Cookie("scope", scopeRequested);
      scopeCookie.setPath("/");
      scopeCookie.setMaxAge((int) SCOPE_COOKIE_NEVER_EXPIRES);
      response.addCookie(scopeCookie);
    }

    return MoreObjects.firstNonNull(scopeRequested, "scopes");
  }

  private String getScopesKeyFromCookie(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return null;
    }

    for (Cookie cookie : cookies) {
      if (cookie.getName().equalsIgnoreCase("scope")) {
        return cookie.getValue();
      }
    }
    return null;
  }

  private SortedSet<Scope> getScopes(String baseScopeKey, Scope... scopes) {
    HashSet<Scope> fullScopes = new HashSet<Scope>(scopesForKey(baseScopeKey));
    fullScopes.addAll(Arrays.asList(scopes));

    return new TreeSet<Scope>(fullScopes);
  }

  private List<Scope> scopesForKey(String baseScopeKey) {
    return MoreObjects
        .firstNonNull(config.scopes.get(baseScopeKey), DEFAULT_SCOPES);
  }
}
