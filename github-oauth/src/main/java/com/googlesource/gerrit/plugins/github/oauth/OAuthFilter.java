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

import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.inject.Inject;

public class OAuthFilter implements Filter {
  private static final org.slf4j.Logger log = LoggerFactory
      .getLogger(OAuthFilter.class);

  private final OAuthConfig config;
  private final OAuthCookieProvider cookieProvider;
  private final OAuthProtocol oauth;

  @Inject
  public OAuthFilter(OAuthConfig config) {
    this.config = config;
    this.cookieProvider = new OAuthCookieProvider();
    this.oauth =
        new OAuthProtocol(config, GitHubHttpProvider.getInstance().get(),
            new Gson());
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    cookieProvider.init();
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
    log.info("doFilter(" + httpRequest.getRequestURI() + ")");

    OAuthCookie authCookie = getOAuthCookie(httpRequest);
    String targetUrl = httpRequest.getParameter("state");

    if (authCookie == null) {
      if (oauth.isOAuthFinal(httpRequest)) {

        String user =
            oauth.loginPhase2(httpRequest, httpResponse).hub.getMyself()
                .getLogin();

        if (user != null) {
          httpResponse.addCookie(cookieProvider.getFromUser(user));
          httpResponse.sendRedirect(targetUrl);
          return;
        } else {
          httpResponse.sendError(HttpURLConnection.HTTP_UNAUTHORIZED,
              "Login failed");
        }
      } else {
        if(oauth.isOAuthLogin(httpRequest)) {
        oauth.loginPhase1(httpRequest, httpResponse);
        } else {
          chain.doFilter(request, response);
        }
      }
      return;
    } else {
      HttpServletRequest wrappedRequest =
          new AuthenticatedHttpRequest(httpRequest, config.httpHeader,
              authCookie.user);

      if (targetUrl != null && oauth.isOAuthFinal(httpRequest)) {
        httpResponse.sendRedirect(config.getUrl(targetUrl,
            OAuthConfig.OAUTH_FINAL) + "?code=" + request.getParameter("code"));
        return;
      } else {
        chain.doFilter(wrappedRequest, response);
      }
    }
  }

  private OAuthCookie getOAuthCookie(HttpServletRequest request) {
    for (Cookie cookie : request.getCookies()) {
      if (cookie.getName().equalsIgnoreCase(OAuthCookie.OAUTH_COOKIE_NAME)
          && !Strings.isNullOrEmpty(cookie.getValue())) {
        return cookieProvider.getFromCookie(cookie);
      }
    }
    return null;
  }

  @Override
  public void destroy() {
    log.info("Init");
  }
}
