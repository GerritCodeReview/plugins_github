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


import com.google.gerrit.server.config.SitePaths;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.kohsuke.github.GHMyself;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Singleton
public class OAuthWebFilter implements Filter {
  private static final org.slf4j.Logger log = LoggerFactory
      .getLogger(OAuthWebFilter.class);
  public static final String GERRIT_COOKIE_NAME = "GerritAccount";
  public static final String GITHUB_EXT_ID = "github_oauth:";

  private final GitHubOAuthConfig config;
  private final Random retryRandom = new Random(System.currentTimeMillis());
  private SitePaths sites;
  private ScopedProvider<GitHubLogin> loginProvider;

  @Inject
  public OAuthWebFilter(GitHubOAuthConfig config, SitePaths sites,
  // We need to explicitly tell Guice the correct implementation
  // as this filter is instantiated with a standard Gerrit WebModule
      GitHubLogin.Provider loginProvider) {
    this.config = config;
    this.sites = sites;
    this.loginProvider = loginProvider;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response,
      FilterChain chain) throws IOException, ServletException {

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;
    log.debug("OAuthWebFilter(" + httpRequest.getRequestURL() + ") code="
        + request.getParameter("code"));

    Cookie gerritCookie = getGerritCookie(httpRequest);
    try {
      GitHubLogin ghLogin = loginProvider.get(httpRequest);

      if (OAuthProtocol.isOAuthLogout(httpRequest)) {
        logout(request, response, chain, httpRequest);
      } else if (OAuthProtocol.isOAuthRequest(httpRequest)
          && !ghLogin.isLoggedIn()) {
        login(request, httpRequest, httpResponse, ghLogin);
      } else {
        if (ghLogin != null && ghLogin.isLoggedIn()) {
          httpRequest =
              new AuthenticatedHttpRequest(httpRequest, config.httpHeader,
                  ghLogin.getMyself().getLogin(),
                  config.oauthHttpHeader,
                  GITHUB_EXT_ID + ghLogin.getToken().accessToken);
        }

        if (OAuthProtocol.isOAuthFinalForOthers(httpRequest)) {
          httpResponse.sendRedirect(OAuthProtocol
              .getTargetOAuthFinal(httpRequest));
        } else {
          chain.doFilter(httpRequest, response);
        }
      }
    } finally {
      HttpSession httpSession = httpRequest.getSession();
      if (gerritCookie != null && httpSession != null) {
        String gerritCookieValue = gerritCookie.getValue();
        String gerritSessionValue =
            (String) httpSession.getAttribute("GerritAccount");

        if (gerritSessionValue == null) {
          httpSession.setAttribute("GerritAccount", gerritCookieValue);
        } else if (!gerritSessionValue.equals(gerritCookieValue)) {
          httpSession.invalidate();
        }
      }
    }
  }

  private void login(ServletRequest request, HttpServletRequest httpRequest,
      HttpServletResponse httpResponse, GitHubLogin ghLogin) throws IOException {
    if (ghLogin.login(httpRequest, httpResponse)) {
      GHMyself myself = ghLogin.getMyself();
      String user = myself.getLogin();

      updateSecureConfigWithRetry(ghLogin.hub.getMyOrganizations().keySet(),
          user, ghLogin.token.accessToken);
    }
  }

  private void logout(ServletRequest request, ServletResponse response,
      FilterChain chain, HttpServletRequest httpRequest) throws IOException,
      ServletException {
    getGitHubLogin(request).logout();
    GitHubLogoutServletResponse bufferedResponse =
        new GitHubLogoutServletResponse((HttpServletResponse) response,
            config.logoutRedirectUrl);
    chain.doFilter(httpRequest, bufferedResponse);
  }

  private GitHubLogin getGitHubLogin(ServletRequest request) {
    return loginProvider.get((HttpServletRequest) request);
  }

  private void updateSecureConfigWithRetry(Set<String> organisations,
      String user, String access_token) {
    int retryCount = 0;

    while (retryCount < config.fileUpdateMaxRetryCount) {
      try {
        updateSecureConfig(organisations, user, access_token);
        return;
      } catch (IOException e) {
        retryCount++;
        int retryInterval =
            retryRandom.nextInt(config.fileUpdateMaxRetryIntervalMsec);
        log.warn("Error whilst trying to update "
            + sites.secure_config
            + (retryCount < config.fileUpdateMaxRetryCount ? ": attempt #"
                + retryCount + " will be retried after " + retryInterval
                + " msecs" : ""), e);
        try {
          Thread.sleep(retryInterval);
        } catch (InterruptedException e1) {
          log.error("Thread has been cancelled before retrying to save "
              + sites.secure_config);
          return;
        }
      } catch (ConfigInvalidException e) {
        log.error("Cannot update " + sites.secure_config
            + " as the file is corrupted", e);
        return;
      }
    }
  }

  private synchronized void updateSecureConfig(Set<String> organisations,
      String user, String access_token) throws IOException,
      ConfigInvalidException {
    FileBasedConfig currentSecureConfig =
        new FileBasedConfig(sites.secure_config, FS.DETECTED);
    long currentSecureConfigUpdateTs = sites.secure_config.lastModified();
    currentSecureConfig.load();

    boolean configUpdate =
        updateConfigSection(currentSecureConfig, user, user, access_token);
    for (String organisation : organisations) {
      configUpdate |=
          updateConfigSection(currentSecureConfig, organisation, user,
              access_token);
    }

    if (!configUpdate) {
      return;
    }

    log.info("Updating " + sites.secure_config + " credentials for user "
        + user);

    if (sites.secure_config.lastModified() != currentSecureConfigUpdateTs) {
      throw new ConcurrentFileBasedConfigWriteException("File "
          + sites.secure_config + " was written at "
          + formatTS(sites.secure_config.lastModified())
          + " while was trying to update security for user " + user);
    }
    currentSecureConfig.save();
  }

  private String formatTS(long ts) {
    return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(ts));
  }

  private boolean updateConfigSection(FileBasedConfig config, String section,
      String user, String password) {
    String configUser = config.getString("remote", section, "username");
    String configPassword = config.getString("remote", section, "password");
    if (!StringUtils.equals(configUser, user)
        || StringUtils.equals(configPassword, password)) {
      return false;
    }

    config.setString("remote", section, "username", user);
    config.setString("remote", section, "password", password);
    return true;
  }

  private Cookie getGerritCookie(HttpServletRequest httpRequest) {
    for (Cookie cookie : getCookies(httpRequest)) {
      if (cookie.getName().equalsIgnoreCase(GERRIT_COOKIE_NAME)) {
        return cookie;
      }
    }
    return null;
  }

  private Cookie[] getCookies(HttpServletRequest httpRequest) {
    Cookie[] cookies = httpRequest.getCookies();
    return cookies == null ? new Cookie[0] : cookies;
  }

  @Override
  public void destroy() {
    log.info("Init");
  }
}
