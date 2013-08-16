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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.HttpClient;
import org.kohsuke.github.GHMyself;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

@Singleton
public class OAuthFilter implements Filter {
  private static final org.slf4j.Logger log = LoggerFactory
      .getLogger(OAuthFilter.class);
  private static final String GERRIT_COOKIE_NAME = "GerritAccount";

  private final OAuthConfig config;
  private final OAuthCookieProvider cookieProvider;
  private final OAuthProtocol oauth;

  @Inject
  public OAuthFilter(OAuthConfig config) {
    this.config = config;
    this.cookieProvider = new OAuthCookieProvider(new TokenCipher());
    HttpClient httpClient;
    httpClient = new GitHubHttpProvider().get();
    this.oauth = new OAuthProtocol(config, httpClient, new Gson());
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
    log.info("doFilter(" + httpRequest.getRequestURI() + ") code="
        + request.getParameter("code") + " me=" + oauth.me());

    Cookie gerritCookie = getGerritCookie(httpRequest);
    OAuthCookie authCookie =
        getOAuthCookie(httpRequest, (HttpServletResponse) response);

    if (((oauth.isOAuthLogin(httpRequest) || oauth.isOAuthFinal(httpRequest)) && authCookie == null)
        || (authCookie == null && gerritCookie == null)) {
      if (oauth.isOAuthFinal(httpRequest)) {

        GHMyself myself =
            oauth.loginPhase2(httpRequest, httpResponse).hub.getMyself();
        String user = myself.getLogin();
        String email = myself.getEmail();
        String fullName =
            Strings.emptyToNull(myself.getName()) == null ? user : myself
                .getName();

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
