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

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.github.oauth.OAuthProtocol.Scope;
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
import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.HttpException;
import org.kohsuke.github.connector.GitHubConnector;
import org.kohsuke.github.internal.GitHubConnectorHttpConnectorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitHubLogin implements Serializable {
  private static final long serialVersionUID = 1L;
  private static final Logger log = LoggerFactory.getLogger(GitHubLogin.class);
  private static final List<Scope> DEFAULT_SCOPES =
      Arrays.asList(Scope.PUBLIC_REPO, Scope.USER_EMAIL, Scope.READ_ORG);
  private static final long SCOPE_COOKIE_NEVER_EXPIRES = DAYS.toSeconds(50 * 365);

  @Singleton
  public static class Provider extends HttpSessionProvider<GitHubLogin> {
    @Override
    public GitHubLogin get(HttpServletRequest request) {
      return super.get(request);
    }
  }

  private String accessToken;

  private String state;

  private SortedSet<Scope> loginScopes;
  private final GitHubOAuthConfig config;
  private final CanonicalWebUrls canonicalWebUrls;
  private final VirtualDomainConfig virtualDomainConfig;
  private final GitHubConnector gitHubConnector;

  public String getAccessToken() {
    return accessToken;
  }

  public GHMyself getMyself() throws IOException {
    if (isLoggedIn()) {
      return new GitHubMyselfWrapper(getHub().getMyself());
    }
    return null;
  }

  public Set<String> getMyOrganisationsLogins(String username) throws IOException {
    if (isLoggedIn()) {
      try {
        return getHub().getMyOrganizations().keySet();
      } catch (HttpException httpException) {
        if (!httpException.getMessage().contains("You need at least")) {
          throw httpException;
        }
        log.info(
            "Cannot access organizations for user '{}': falling back to list of public"
                + " organisations",
            username);

        return getHub().getUserPublicOrganizations(username).keySet();
      }
    }
    return Collections.emptySet();
  }

  @Inject
  public GitHubLogin(
      GitHubOAuthConfig config,
      CanonicalWebUrls canonicalWebUrls,
      VirtualDomainConfig virutalDomainConfig,
      GitHubHttpConnector httpConnector) {
    this.config = config;
    this.canonicalWebUrls = canonicalWebUrls;
    this.virtualDomainConfig = virutalDomainConfig;
    this.gitHubConnector = GitHubConnectorHttpConnectorAdapter.adapt(httpConnector);
  }

  public boolean isLoggedIn() {
    return accessToken != null;
  }

  public void login(
      HttpServletRequest request,
      HttpServletResponse response,
      OAuthProtocol oauth,
      Scope... scopes)
      throws IOException {

    log.debug("Login " + this);
    if (OAuthProtocol.isOAuthFinal(request)) {
      log.debug("Login-FINAL " + this);
      login(oauth.loginPhase2(request, response, state).accessToken);
      this.state = ""; // Make sure state is used only once

      if (isLoggedIn()) {
        log.debug("Login-SUCCESS " + this);
        response.sendRedirect(OAuthProtocol.getTargetUrl(request));
      }
    } else {
      Set<ScopeKey> configuredScopesProfiles = virtualDomainConfig.getScopes(request).keySet();
      String scopeRequested = getScopesKey(request, response);
      if (Strings.isNullOrEmpty(scopeRequested) && configuredScopesProfiles.size() > 1) {
        response.sendRedirect(canonicalWebUrls.getScopeSelectionUrl());
      } else {
        this.loginScopes =
            getScopes(request, MoreObjects.firstNonNull(scopeRequested, "scopes"), scopes);
        log.debug("Login-PHASE1 " + this);
        state = oauth.loginPhase1(request, response, loginScopes);
      }
    }
  }

  public void logout() {
    accessToken = null;
  }

  public GitHub login(String authAccessToken) throws IOException {
    log.debug("Logging in using access token {}", authAccessToken);
    this.accessToken = authAccessToken;
    return getHub();
  }

  @Override
  public String toString() {
    return "GitHubLogin [token=" + accessToken + ", scopes=" + loginScopes + "]";
  }

  public GitHub getHub() throws IOException {
    if (accessToken == null) {
      return null;
    }
    return new GitHubBuilder()
        .withEndpoint(config.gitHubApiUrl)
        .withOAuthToken(accessToken)
        .withConnector(gitHubConnector)
        .build();
  }

  private String getScopesKey(HttpServletRequest request, HttpServletResponse response) {
    String scopeRequested = request.getParameter("scope");
    if (scopeRequested == null) {
      scopeRequested = getScopesKeyFromCookie(request);
    }

    boolean rememberScope =
        Strings.nullToEmpty(request.getParameter("rememberScope")).equalsIgnoreCase("on");
    if (scopeRequested != null && rememberScope) {
      Cookie scopeCookie = new Cookie("scope", scopeRequested);
      scopeCookie.setPath("/");
      scopeCookie.setMaxAge((int) SCOPE_COOKIE_NEVER_EXPIRES);
      config.getCookieDomain().ifPresent(scopeCookie::setDomain);
      response.addCookie(scopeCookie);
    }

    return scopeRequested;
  }

  public String getScopesKeyFromCookie(HttpServletRequest request) {
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

  private SortedSet<Scope> getScopes(HttpServletRequest req, String baseScopeKey, Scope... scopes) {
    HashSet<Scope> fullScopes = new HashSet<>(scopesForKey(req, baseScopeKey));
    fullScopes.addAll(Arrays.asList(scopes));

    return new TreeSet<>(fullScopes);
  }

  private List<Scope> scopesForKey(HttpServletRequest req, String baseScopeKey) {
    return virtualDomainConfig.getScopes(req).entrySet().stream()
        .filter(entry -> entry.getKey().name().equals(baseScopeKey))
        .map(entry -> entry.getValue())
        .findFirst()
        .orElse(DEFAULT_SCOPES);
  }
}
