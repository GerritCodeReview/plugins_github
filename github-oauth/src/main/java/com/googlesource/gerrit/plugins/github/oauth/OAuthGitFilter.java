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

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.Iterators;
import com.google.gerrit.httpd.XGerritAuth;
import com.google.gerrit.server.account.AccountCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.googlesource.gerrit.plugins.github.oauth.OAuthProtocol.AccessToken;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

@Singleton
public class OAuthGitFilter implements Filter {
  private static final String GITHUB_X_OAUTH_BASIC = "x-oauth-basic";
  private static final org.slf4j.Logger log = LoggerFactory
      .getLogger(OAuthGitFilter.class);
  public static final String GIT_REALM_NAME =
      "GitHub authentication for Gerrit Code Review";
  private static final String GIT_AUTHORIZATION_HEADER = "Authorization";
  private static final String GIT_AUTHENTICATION_BASIC = "Basic ";

  private final OAuthCache oauthCache;
  private final AccountCache accountCache;
  private final GitHubHttpProvider httpClientProvider;
  private final GitHubOAuthConfig config;
  private final XGerritAuth xGerritAuth;
  private ScopedProvider<GitHubLogin> ghLoginProvider;

  public static class BasicAuthHttpRequest extends HttpServletRequestWrapper {
    private HashMap<String, String> headers = new HashMap<String, String>();

    public BasicAuthHttpRequest(HttpServletRequest request, String username,
        String password) {
      super(request);

      try {
        headers.put(
            GIT_AUTHORIZATION_HEADER,
            GIT_AUTHENTICATION_BASIC
                + Base64.encodeBase64String((username + ":" + password)
                    .getBytes(OAuthGitFilter.encoding(request))));
      } catch (UnsupportedEncodingException e) {
        // This cannot really happen as we have already used the encoding for
        // decoding the request
      }
    }

    @Override
    public Enumeration<String> getHeaderNames() {
      final Enumeration<String> wrappedHeaderNames = super.getHeaderNames();
      HashSet<String> headerNames = new HashSet<String>(headers.keySet());
      while (wrappedHeaderNames.hasMoreElements()) {
        headerNames.add(wrappedHeaderNames.nextElement());
      }
      return Iterators.asEnumeration(headerNames.iterator());
    }

    @Override
    public String getHeader(String name) {
      String headerValue = headers.get(name);
      if (headerValue != null) {
        return headerValue;
      } else {
        return super.getHeader(name);
      }
    }
  }

  @Inject
  public OAuthGitFilter(OAuthCache oauthCache, AccountCache accountCache,
      GitHubHttpProvider httpClientProvider, GitHubOAuthConfig config,
      XGerritAuth xGerritAuth, GitHubLogin.Provider ghLoginProvider) {
    this.oauthCache = oauthCache;
    this.accountCache = accountCache;
    this.httpClientProvider = httpClientProvider;
    this.config = config;
    this.xGerritAuth = xGerritAuth;
    this.ghLoginProvider = ghLoginProvider;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response,
      FilterChain chain) throws IOException, ServletException {

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse =
        new OAuthGitWrappedResponse((HttpServletResponse) response);
    GitHubLogin ghLogin = ghLoginProvider.get(httpRequest);
    log.debug("OAuthGitFilter(" + httpRequest.getRequestURL() + ") ghLogin="
        + ghLogin);

    String username =
        getAuthenticatedUserFromGitRequestUsingOAuthToken(httpRequest,
            httpResponse);
    if (username != null) {
      String gerritPassword =
          accountCache.getByUsername(username).getPassword(username);

      if (gerritPassword == null) {
        gerritPassword =
            generateRandomGerritPassword(username, httpRequest, httpResponse,
                chain);
        if (Strings.isNullOrEmpty(gerritPassword)) {
          httpResponse.sendError(SC_FORBIDDEN,
              "Unable to generate Gerrit password for Git Basic-Auth");
        } else {
          httpResponse.sendRedirect(getRequestPathWithQueryString(httpRequest));
        }
        return;
      }

      httpRequest =
          new BasicAuthHttpRequest(httpRequest, username, gerritPassword);
    }

    chain.doFilter(httpRequest, httpResponse);
  }

  private String getRequestPathWithQueryString(HttpServletRequest httpRequest) {
    String requestPathWithQueryString =
        httpRequest.getContextPath() + httpRequest.getServletPath()
            + Strings.nullToEmpty(httpRequest.getPathInfo()) + "?"
            + httpRequest.getQueryString();
    return requestPathWithQueryString;
  }

  private String generateRandomGerritPassword(String username,
      HttpServletRequest httpRequest, HttpServletResponse httpResponse,
      FilterChain chain) throws IOException, ServletException {
    log.warn("User " + username + " has not a Gerrit HTTP password: "
        + "generating a random one in order to be able to use Git over HTTP");
    Cookie gerritCookie =
        getGerritLoginCookie(username, httpRequest, httpResponse, chain);
    String xGerritAuthValue = xGerritAuth.getAuthValue(gerritCookie);

    HttpPut putRequest =
        new HttpPut(getRequestUrlWithAlternatePath(httpRequest,
            "/accounts/self/password.http"));
    putRequest.setHeader("Cookie",
        gerritCookie.getName() + "=" + gerritCookie.getValue());
    putRequest.setHeader(XGerritAuth.X_GERRIT_AUTH, xGerritAuthValue);

    putRequest.setEntity(new StringEntity("{\"generate\":true}",
        ContentType.APPLICATION_JSON));
    HttpResponse putResponse = httpClientProvider.get().execute(putRequest);
    if (putResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
      throw new ServletException(
          "Cannot generate HTTP password for authenticating user " + username);
    }

    return accountCache.getByUsername(username).getPassword(username);
  }

  private URI getRequestUrlWithAlternatePath(HttpServletRequest httpRequest,
      String alternatePath) throws MalformedURLException {
    URL originalUrl = new URL(httpRequest.getRequestURL().toString());
    String contextPath = httpRequest.getContextPath();
    return URI.create(originalUrl.getProtocol() + "://" + originalUrl.getHost()
        + ":" + getPort(originalUrl) + contextPath + alternatePath);
  }

  private int getPort(URL originalUrl) {
    String protocol = originalUrl.getProtocol().toLowerCase();
    int port = originalUrl.getPort();
    if (port == -1) {
      return protocol.equals("https") ? 443 : 80;
    } else {
      return port;
    }
  }

  private Cookie getGerritLoginCookie(String username,
      HttpServletRequest httpRequest, HttpServletResponse httpResponse,
      FilterChain chain) throws IOException, ServletException {
    AuthenticatedPathHttpRequest loginRequest =
        new AuthenticatedLoginHttpRequest(httpRequest, config.httpHeader,
            username);
    AuthenticatedLoginHttpResponse loginResponse =
        new AuthenticatedLoginHttpResponse(httpResponse);
    chain.doFilter(loginRequest, loginResponse);
    return loginResponse.getGerritCookie();
  }

  private String getAuthenticatedUserFromGitRequestUsingOAuthToken(
      HttpServletRequest req, HttpServletResponse rsp) throws IOException {
    final String httpBasicAuth = getHttpBasicAuthenticationHeader(req);
    if (httpBasicAuth == null) {
      return null;
    }

    if (isInvalidHttpAuthenticationHeader(httpBasicAuth)) {
      rsp.sendError(SC_UNAUTHORIZED);
      return null;
    }

    String oauthToken = StringUtils.substringBefore(httpBasicAuth, ":");
    String oauthKeyword = StringUtils.substringAfter(httpBasicAuth, ":");
    if (Strings.isNullOrEmpty(oauthToken)
        || Strings.isNullOrEmpty(oauthKeyword)) {
      rsp.sendError(SC_UNAUTHORIZED);
      return null;
    }

    if (!oauthKeyword.equalsIgnoreCase(GITHUB_X_OAUTH_BASIC)) {
      return null;
    }

    boolean loginSuccessful = false;
    String oauthLogin = null;
    try {
      oauthLogin =
          oauthCache.getLoginByAccessToken(new AccessToken(oauthToken));
      loginSuccessful = !Strings.isNullOrEmpty(oauthLogin);
    } catch (ExecutionException e) {
      log.warn("Login failed for OAuth token " + oauthToken, e);
      loginSuccessful = false;
    }

    if (!loginSuccessful) {
      rsp.sendError(SC_FORBIDDEN);
      return null;
    }

    return oauthLogin;
  }


  private boolean isInvalidHttpAuthenticationHeader(String usernamePassword) {
    return usernamePassword.indexOf(':') < 1;
  }

  static String encoding(HttpServletRequest req) {
    return MoreObjects.firstNonNull(req.getCharacterEncoding(), "UTF-8");
  }

  private String getHttpBasicAuthenticationHeader(final HttpServletRequest req)
      throws UnsupportedEncodingException {
    String hdr = req.getHeader(GIT_AUTHORIZATION_HEADER);
    if (hdr == null || !hdr.startsWith(GIT_AUTHENTICATION_BASIC)) {
      return null;
    } else {
      return new String(Base64.decodeBase64(hdr
          .substring(GIT_AUTHENTICATION_BASIC.length())), encoding(req));
    }
  }

  @Override
  public void destroy() {
    log.info("Destroy");
  }
}
