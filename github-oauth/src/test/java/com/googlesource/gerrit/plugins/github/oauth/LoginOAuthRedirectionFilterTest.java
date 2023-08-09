// Copyright (C) 2021 The Android Open Source Project
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URLEncoder;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LoginOAuthRedirectionFilterTest {
  @Mock private HttpServletRequest httpServletRequest;
  @Mock private HttpServletResponse httpServletResponse;
  @Mock private FilterChain filterChain;

  private static String SOURCE_HOST1 = "my-subdomain-1.my-domain.com";
  private static String SOURCE_HOST2 = "my-subdomain-2.my-domain.com";
  private static String SOURCE_SSL_PORT = "443";
  private static String SOURCE_HTTPS_PROTOCOL = "https";

  private final String canonicalUrl = "https://my-subdomain-1.my-domain.com/";

  @Test
  public void shouldGoNextInChainWhenXForwardedHeadersNotPresent()
      throws IOException, ServletException {
    LoginOAuthRedirectionFilter loginOAuthRedirection =
        new LoginOAuthRedirectionFilter(canonicalUrl);
    loginOAuthRedirection.doFilter(httpServletRequest, httpServletResponse, filterChain);
    verify(httpServletRequest).getHeader(LoginOAuthRedirectionFilter.X_FORWARDED_HOST_HTTP_HEADER);
    verify(httpServletRequest).getHeader(LoginOAuthRedirectionFilter.X_FORWARDED_PORT_HTTP_HEADER);
    verify(httpServletRequest).getHeader(LoginOAuthRedirectionFilter.X_FORWARDED_PROTO_HTTP_HEADER);
    verify(filterChain).doFilter(httpServletRequest, httpServletResponse);
  }

  @Test
  public void shouldRedirectToLoginWhenSourceDomainAndCanonicalWebDomainAreTheSame()
      throws IOException, ServletException {
    when(httpServletRequest.getHeader(LoginOAuthRedirectionFilter.X_FORWARDED_HOST_HTTP_HEADER))
        .thenReturn(SOURCE_HOST1);
    when(httpServletRequest.getHeader(LoginOAuthRedirectionFilter.X_FORWARDED_PORT_HTTP_HEADER))
        .thenReturn(SOURCE_SSL_PORT);
    when(httpServletRequest.getHeader(LoginOAuthRedirectionFilter.X_FORWARDED_PROTO_HTTP_HEADER))
        .thenReturn(SOURCE_HTTPS_PROTOCOL);
    LoginOAuthRedirectionFilter loginOAuthRedirection =
        new LoginOAuthRedirectionFilter(canonicalUrl);
    loginOAuthRedirection.doFilter(httpServletRequest, httpServletResponse, filterChain);
    verify(httpServletResponse).sendRedirect(GitHubOAuthConfig.GERRIT_LOGIN);
  }

  @Test
  public void
      shouldRedirectToLoginWithFinalRedirectParamWhenSourceDomainAndCanonicalWebDomainAreNotTheSame()
          throws IOException, ServletException {
    when(httpServletRequest.getHeader(LoginOAuthRedirectionFilter.X_FORWARDED_HOST_HTTP_HEADER))
        .thenReturn(SOURCE_HOST2);
    when(httpServletRequest.getHeader(LoginOAuthRedirectionFilter.X_FORWARDED_PORT_HTTP_HEADER))
        .thenReturn(SOURCE_SSL_PORT);
    when(httpServletRequest.getHeader(LoginOAuthRedirectionFilter.X_FORWARDED_PROTO_HTTP_HEADER))
        .thenReturn(SOURCE_HTTPS_PROTOCOL);
    LoginOAuthRedirectionFilter loginOAuthRedirection =
        new LoginOAuthRedirectionFilter(canonicalUrl);
    loginOAuthRedirection.doFilter(httpServletRequest, httpServletResponse, filterChain);
    String finalRedirectUrlEncoded =
        URLEncoder.encode(
            SOURCE_HTTPS_PROTOCOL + "://" + SOURCE_HOST2 + ":" + SOURCE_SSL_PORT + "/", "UTF-8");
    verify(httpServletResponse)
        .sendRedirect(canonicalUrl + "login?final_redirect_url=" + finalRedirectUrlEncoded);
  }
}
