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

import com.google.gerrit.server.config.CanonicalWebUrl;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Optional;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class LoginOAuthRedirectionFilter implements Filter {
  private static final Logger logger = LoggerFactory.getLogger(LoginOAuthRedirectionFilter.class);
  static final String X_FORWARDED_HOST_HTTP_HEADER = "X-Forwarded-Host";
  // TODO to build final_redirection we should:
  // TODO receive X-Forwarded-Proto header
  // TODO receive X-Forwarded-Port header
  private final URL canonicalUrl;

  @Inject
  LoginOAuthRedirectionFilter(@CanonicalWebUrl String canonicalUrl) throws MalformedURLException {
    this.canonicalUrl = new URL(canonicalUrl);
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {}

  @Override
  public void doFilter(
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
      throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
    HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
    Optional<String> maybeRedirectURL =
        Optional.ofNullable(httpRequest.getHeader(X_FORWARDED_HOST_HTTP_HEADER))
            .map(xfhHeader -> buildRedirectionUrl(xfhHeader));

    if (maybeRedirectURL.isPresent()) {
      logger.info("Redirecting to " + maybeRedirectURL.get());
      httpResponse.sendRedirect(maybeRedirectURL.get());
    } else {
      filterChain.doFilter(servletRequest, servletResponse);
    }
  }

  @Override
  public void destroy() {}

  private String buildRedirectionUrl(String xfhHeader) {
    if (canonicalUrl.getHost().equalsIgnoreCase(xfhHeader)) {
      return "/login";
    } else {
      // TODO initial weak assumptions:
      // - protocol of the canonical url is the same as the xfhHeader
      // - port is 80
      String finalRedirectUrl = canonicalUrl.getProtocol() + "://" + xfhHeader;
      try {
        return canonicalUrl
            + "login?final_redirect_url="
            + URLEncoder.encode(finalRedirectUrl, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e); // TODO deal with this
      }
    }
  }
}
