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
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GitHub;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class OAuthFilter implements Filter {
  private static final org.slf4j.Logger log = LoggerFactory
      .getLogger(OAuthFilter.class);

  private final OAuthFilterConfig config;
  private final Provider<HttpClient> httpProvider;
  private final Gson gson;
  private final OAuthCookieProvider cookieProvider;

  @Inject
  public OAuthFilter(OAuthFilterConfig config) {
    this.config = config;
    this.httpProvider = GitHubHttpProvider.getInstance();
    gson = new Gson();
    cookieProvider = new OAuthCookieProvider();
    cookieProvider.init();
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response,
      FilterChain chain) throws IOException, ServletException {

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;
    log.info("doFilter(" + httpRequest.getRequestURI() + ")");

    OAuthCookie authCookie = getOAuthCookie(httpRequest);
    if (authCookie == null) {
      if (isOAuthFinal(httpRequest)) {

        String user = loginPhase2(httpRequest, httpResponse);

        if (user != null) {
          httpResponse.addCookie(cookieProvider.getFromUser(user));
          httpResponse.sendRedirect(httpRequest.getParameter("state"));
          return;
        } else {
          httpResponse.sendError(HttpURLConnection.HTTP_UNAUTHORIZED,
              "Login failed");
        }
      } else {
        loginPhase1(httpRequest, httpResponse);
      }
      return;
    } else {
      chain.doFilter(new AuthenticatedHttpRequest(httpRequest, null,
          config.httpHeader, authCookie.user), response);
    }
  }

  private boolean isOAuthFinal(HttpServletRequest request) {
    return request.getRequestURI().equalsIgnoreCase(
        OAuthFilterConfig.OAUTH_FINAL);
  }

  private String loginPhase2(HttpServletRequest request,
      HttpServletResponse response) throws IOException {
    HttpClient http = httpProvider.get();

    HttpPost post = null;

    post = new HttpPost(config.gitHubOAuthAccessTokenUrl);
    post.setHeader("Accept", "application/json");
    List<NameValuePair> nvps = new ArrayList<NameValuePair>();
    nvps.add(new BasicNameValuePair("client_id", config.gitHubClientId));
    nvps.add(new BasicNameValuePair("client_secret", config.gitHubClientSecret));
    nvps.add(new BasicNameValuePair("code", request.getParameter("code")));
    try {
      post.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
    } catch (UnsupportedEncodingException e) {
      // Will never happen
    }

    try {
      HttpResponse postResponse = http.execute(post);
      if (postResponse.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
        log.error("POST " + config.gitHubOAuthAccessTokenUrl
            + " request for access token failed with status "
            + postResponse.getStatusLine());
        response.sendError(HttpURLConnection.HTTP_UNAUTHORIZED,
            "Request for access token not authorised");
        postResponse.getEntity().consumeContent();
        return null;
      }

      OAuthAccessToken token =
          gson.fromJson(new InputStreamReader(postResponse.getEntity()
              .getContent(), "UTF-8"), OAuthAccessToken.class);
      GitHub github = GitHub.connectUsingOAuth(token.access_token);
      GHMyself myself = github.getMyself();
      return myself.getLogin();
    } catch (IOException e) {
      log.error("POST " + config.gitHubOAuthAccessTokenUrl
          + " request for access token failed", e);
      response.sendError(HttpURLConnection.HTTP_UNAUTHORIZED,
          "Request for access token not authorised");
      return null;
    }
  }

  private void loginPhase1(HttpServletRequest request,
      HttpServletResponse response) throws IOException {
    response.sendRedirect(String.format(
        "%s?client_id=%s&redirect_uri=%s&state=%s", config.gitHubOAuthUrl,
        config.gitHubClientId, getURLEncoded(config.oAuthFinalRedirectUrl),
        getURLEncoded(request.getRequestURI().toString())));
  }

  private String getURLEncoded(String url) {
    try {
      return URLEncoder.encode(url, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      // UTF-8 is hardcoded, cannot fail
      return null;
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
