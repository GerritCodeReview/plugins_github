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
import java.net.HttpURLConnection;
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

import org.apache.http.client.HttpClient;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GitHub;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.gerrit.server.config.SitePaths;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class OAuthFilter implements Filter {
  private static final org.slf4j.Logger log = LoggerFactory
      .getLogger(OAuthFilter.class);
  private static final String GERRIT_COOKIE_NAME = "GerritAccount";

  private final GitHubOAuthConfig config;
  private final OAuthCookieProvider cookieProvider;
  private final OAuthProtocol oauth;
  private final Random retryRandom = new Random(System.currentTimeMillis());
  private SitePaths sites;

  @Inject
  public OAuthFilter(GitHubOAuthConfig config, SitePaths sites) {
    this.config = config;
    this.cookieProvider = new OAuthCookieProvider(new TokenCipher());
    HttpClient httpClient;
    httpClient = new GitHubHttpProvider().get();
    this.oauth = new OAuthProtocol(config, httpClient, new Gson());
    this.sites = sites;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response,
      FilterChain chain) throws IOException, ServletException {
    if (!config.enabled) {
      chain.doFilter(request, response);
      return;
    }

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;
    log.debug("doFilter(" + httpRequest.getRequestURI() + ") code="
        + request.getParameter("code") + " me=" + oauth.me());

    Cookie gerritCookie = getGerritCookie(httpRequest);
    try {
      OAuthCookie authCookie =
          getOAuthCookie(httpRequest, (HttpServletResponse) response);
      
      if(oauth.isOAuthLogout((HttpServletRequest) request)) {
        GitHubLogoutServletResponse bufferedResponse = new GitHubLogoutServletResponse((HttpServletResponse) response,
            config.logoutRedirectUrl);
        chain.doFilter(httpRequest, bufferedResponse);
      } else if (((oauth.isOAuthLogin(httpRequest) || oauth.isOAuthFinal(httpRequest)) && authCookie == null)
          || (authCookie == null && gerritCookie == null)) {
        if (oauth.isOAuthFinal(httpRequest)) {

          GitHubLogin hubLogin = oauth.loginPhase2(httpRequest, httpResponse);
          GitHub hub = hubLogin.hub;
          GHMyself myself =
              hub.getMyself();
          String user = myself.getLogin();
          String email = myself.getEmail();
          String fullName =
              Strings.emptyToNull(myself.getName()) == null ? user : myself
                  .getName();

          updateSecureConfigWithRetry(hub.getMyOrganizations().keySet(), user,
              hubLogin.token.access_token);

          if (user != null) {
            OAuthCookie userCookie =
                cookieProvider.getFromUser(user, email, fullName);
            httpResponse.addCookie(userCookie);
            httpResponse.sendRedirect(oauth.getTargetUrl(request));
            return;
          } else {
            httpResponse.sendError(HttpURLConnection.HTTP_UNAUTHORIZED,
                "Login failed");
          }
        } else {
          if (oauth.isOAuthLogin(httpRequest)) {
            oauth.loginPhase1(httpRequest, httpResponse);
          } else {
            chain.doFilter(request, response);
          }
        }
        return;
      } else {
        if (gerritCookie != null && !oauth.isOAuthLogin(httpRequest)) {
          if (authCookie != null) {
            authCookie.setMaxAge(0);
            authCookie.setValue("");
            httpResponse.addCookie(authCookie);
          }
        } else if (authCookie != null) {
          httpRequest =
              new AuthenticatedHttpRequest(httpRequest, config.httpHeader,
                  authCookie.user, config.httpDisplaynameHeader,
                  authCookie.fullName, config.httpEmailHeader, authCookie.email);
        }

        if (oauth.isOAuthFinalForOthers(httpRequest)) {
          httpResponse.sendRedirect(oauth.getTargetOAuthFinal(httpRequest));
          return;
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
        log.warn("Error whilst trying to update " + sites.secure_config
            + (retryCount < config.fileUpdateMaxRetryCount ? ": attempt #" + retryCount + " will be retried after " + retryInterval + " msecs":""), e);
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

    boolean configUpdate = updateConfigSection(currentSecureConfig, user, user, access_token);
    for (String organisation : organisations) {
      configUpdate |= updateConfigSection(currentSecureConfig, organisation, user, access_token);
    }

    if(!configUpdate) {
      return;
    }

    log.info("Updating " + sites.secure_config + " credentials for user " + user);

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

  private boolean updateConfigSection(FileBasedConfig config,
      String section, String user, String password) {
    String configUser = config.getString("remote", section, "username");
    String configPassword = config.getString("remote", section, "password");
    if(configUser == null || !configUser.equals(user) || configPassword.equals(password)) {
      return false;
    }

    config.setString("remote", section, "username", user);
    config.setString("remote", section, "password", password);
    return true;
  }

  private Cookie getGerritCookie(HttpServletRequest httpRequest) {
    for (Cookie cookie : httpRequest.getCookies()) {
      if (cookie.getName().equalsIgnoreCase(GERRIT_COOKIE_NAME)) {
        return cookie;
      }
    }
    return null;
  }

  private OAuthCookie getOAuthCookie(HttpServletRequest request,
      HttpServletResponse response) {
    for (Cookie cookie : request.getCookies()) {
      if (cookie.getName().equalsIgnoreCase(OAuthCookie.OAUTH_COOKIE_NAME)
          && !Strings.isNullOrEmpty(cookie.getValue())) {
        try {
          return cookieProvider.getFromCookie(cookie);
        } catch (OAuthTokenException e) {
          log.warn(
              "Invalid cookie detected: cleaning up and sending a reset back to the browser",
              e);
          cookie.setValue("");
          cookie.setPath("/");
          cookie.setMaxAge(0);
          response.addCookie(cookie);
          return null;
        }
      }
    }
    return null;
  }

  @Override
  public void destroy() {
    log.info("Init");
  }
}
