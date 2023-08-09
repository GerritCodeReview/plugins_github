// Copyright (C) 2023 The Android Open Source Project
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

import com.google.common.net.HttpHeaders;
import com.google.gerrit.server.config.CanonicalWebUrl;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Optional;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Singleton
public class LoginOAuthRedirectionFilter implements Filter {
  public static final String FINAL_REDIRECT_URL = "final_redirect_url";
  private final URL canonicalWebUrl;

  @Inject
  LoginOAuthRedirectionFilter(@CanonicalWebUrl String canonicalWebUrl)
      throws MalformedURLException {
    this.canonicalWebUrl = new URL(canonicalWebUrl);
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {}

  @Override
  public void doFilter(
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
      throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
    HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

    Optional<String> maybeForwardedHost =
        Optional.ofNullable(httpRequest.getHeader(HttpHeaders.X_FORWARDED_HOST));
    Optional<Integer> maybeForwardedPort =
        Optional.ofNullable(httpRequest.getHeader(HttpHeaders.X_FORWARDED_PORT))
            .map(Integer::parseInt);
    Optional<String> maybeForwardedProto =
        Optional.ofNullable(httpRequest.getHeader(HttpHeaders.X_FORWARDED_PROTO));

    if (maybeForwardedHost.isPresent()) {
      if (canonicalWebUrl.getHost().equalsIgnoreCase(maybeForwardedHost.get())) {
        httpResponse.sendRedirect(GitHubOAuthConfig.GERRIT_LOGIN);
        return;
      } else if (maybeForwardedPort.isPresent() && maybeForwardedProto.isPresent()) {
        String loginURL =
            buildLoginURLWithFinalRedirectURL(
                maybeForwardedProto.get(), maybeForwardedHost.get(), maybeForwardedPort.get());
        httpResponse.sendRedirect(loginURL);
        return;
      }
    }
    filterChain.doFilter(servletRequest, servletResponse);
  }

  @Override
  public void destroy() {}

  private String buildLoginURLWithFinalRedirectURL(String proto, String host, int port)
      throws UnsupportedEncodingException, MalformedURLException {
    return canonicalWebUrl
        + "login?"
        + FINAL_REDIRECT_URL
        + "="
        + URLEncoder.encode(new URL(proto, host, port, "/").toString(), "UTF-8");
  }
}
