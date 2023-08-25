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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
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
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.kohsuke.github.GHMyself;
import org.slf4j.LoggerFactory;

@Singleton
public class OAuthWebFilter implements Filter {
  private static final org.slf4j.Logger log = LoggerFactory.getLogger(OAuthWebFilter.class);
  public static final String GERRIT_COOKIE_NAME = "GerritAccount";
  public static final String GITHUB_EXT_ID = "github_oauth:";

  private final GitHubOAuthConfig config;
  private final Random retryRandom = new Random(System.currentTimeMillis());
  private final SitePaths sites;
  private final ScopedProvider<GitHubLogin> loginProvider;
  private final OAuthProtocol oauth;
  private final OAuthTokenCipher oAuthTokenCipher;

  @Inject
  public OAuthWebFilter(
      GitHubOAuthConfig config,
      SitePaths sites,
      OAuthProtocol oauth,
      // We need to explicitly tell Guice the correct implementation
      // as this filter is instantiated with a standard Gerrit WebModule
      GitHubLogin.Provider loginProvider,
      OAuthTokenCipher oAuthTokenCipher) {
    this.config = config;
    this.sites = sites;
    this.oauth = oauth;
    this.loginProvider = loginProvider;
    this.oAuthTokenCipher = oAuthTokenCipher;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {}

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;
    log.debug(
        "OAuthWebFilter(" + httpRequest.getRequestURL() + ") code=" + request.getParameter("code"));

    Cookie gerritCookie = getGerritCookie(httpRequest);
    try {
      GitHubLogin ghLogin = loginProvider.get(httpRequest);

      if (OAuthProtocol.isOAuthRequest(httpRequest)) {
        login(request, httpRequest, httpResponse, ghLogin);
      } else {
        if (OAuthProtocol.isOAuthLogout(httpRequest)) {
          httpResponse = (HttpServletResponse) logout(request, httpResponse, chain, httpRequest);
        }

        if (ghLogin != null && ghLogin.isLoggedIn()) {
          String hashedToken = oAuthTokenCipher.encrypt(ghLogin.getToken().accessToken);
          httpRequest =
              new AuthenticatedHttpRequest(
                  httpRequest,
                  config.httpHeader,
                  ghLogin.getMyself().getLogin(),
                  config.oauthHttpHeader,
                  GITHUB_EXT_ID + hashedToken);
        }

        chain.doFilter(httpRequest, httpResponse);
      }
    } finally {
      HttpSession httpSession = httpRequest.getSession();
      if (gerritCookie != null && httpSession != null) {
        String gerritCookieValue = gerritCookie.getValue();
        String gerritSessionValue = (String) httpSession.getAttribute("GerritAccount");

        if (gerritSessionValue == null) {
          httpSession.setAttribute("GerritAccount", gerritCookieValue);
        } else if (!gerritSessionValue.equals(gerritCookieValue)) {
          httpSession.setAttribute("GerritAccount", null);
          loginProvider.clear(httpRequest);
        }
      }
    }
  }

  private void login(
      ServletRequest request,
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse,
      GitHubLogin ghLogin)
      throws IOException {
    ghLogin.login(httpRequest, httpResponse, oauth);
    if (ghLogin.isLoggedIn()) {
      GHMyself myself = ghLogin.getMyself();
      String user = myself.getLogin();

      updateSecureConfigWithRetry(
          ghLogin.getHub().getMyOrganizations().keySet(), user, ghLogin.getToken().accessToken);
    }
  }

  private ServletResponse logout(
      ServletRequest request,
      ServletResponse response,
      FilterChain chain,
      HttpServletRequest httpRequest) {
    getGitHubLogin(request).logout();
    return new GitHubLogoutServletResponse(
        (HttpServletResponse) response, config.logoutRedirectUrl);
  }

  private GitHubLogin getGitHubLogin(ServletRequest request) {
    return loginProvider.get((HttpServletRequest) request);
  }

  private void updateSecureConfigWithRetry(
      Set<String> organisations, String user, String access_token) {
    int retryCount = 0;

    while (retryCount < config.fileUpdateMaxRetryCount) {
      try {
        updateSecureConfig(organisations, user, access_token);
        return;
      } catch (IOException e) {
        retryCount++;
        int retryInterval = retryRandom.nextInt(config.fileUpdateMaxRetryIntervalMsec);
        log.warn(
            "Error whilst trying to update "
                + sites.secure_config
                + (retryCount < config.fileUpdateMaxRetryCount
                    ? ": attempt #"
                        + retryCount
                        + " will be retried after "
                        + retryInterval
                        + " msecs"
                    : ""),
            e);
        try {
          Thread.sleep(retryInterval);
        } catch (InterruptedException e1) {
          log.error("Thread has been cancelled before retrying to save " + sites.secure_config);
          return;
        }
      } catch (ConfigInvalidException e) {
        log.error("Cannot update " + sites.secure_config + " as the file is corrupted", e);
        return;
      }
    }
  }

  private synchronized void updateSecureConfig(
      Set<String> organisations, String user, String access_token)
      throws IOException, ConfigInvalidException {
    FileBasedConfig currentSecureConfig =
        new FileBasedConfig(sites.secure_config.toFile(), FS.DETECTED);
    FileTime currentSecureConfigUpdateTs = Files.getLastModifiedTime(sites.secure_config);
    currentSecureConfig.load();

    boolean configUpdate = updateConfigSection(currentSecureConfig, user, user, access_token);
    for (String organisation : organisations) {
      configUpdate |= updateConfigSection(currentSecureConfig, organisation, user, access_token);
    }

    if (!configUpdate) {
      return;
    }

    log.info("Updating " + sites.secure_config + " credentials for user " + user);

    FileTime secureConfigCurrentModifiedTime = Files.getLastModifiedTime(sites.secure_config);
    if (!secureConfigCurrentModifiedTime.equals(currentSecureConfigUpdateTs)) {
      throw new ConcurrentFileBasedConfigWriteException(
          "File "
              + sites.secure_config
              + " was written at "
              + secureConfigCurrentModifiedTime
              + " while was trying to update security for user "
              + user);
    }
    currentSecureConfig.save();
  }

  private boolean updateConfigSection(
      FileBasedConfig c, String section, String user, String password) {
    String configUser = c.getString("remote", section, "username");
    String configPassword = c.getString("remote", section, "password");
    if (!StringUtils.equals(configUser, user) || StringUtils.equals(configPassword, password)) {
      return false;
    }

    c.setString("remote", section, "username", user);
    c.setString("remote", section, "password", password);
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
