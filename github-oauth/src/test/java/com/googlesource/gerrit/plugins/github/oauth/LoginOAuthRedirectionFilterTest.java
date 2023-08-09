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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.net.HttpHeaders;
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
  private final String sourceSSLPort = "443";
  private final String sourceHttpsProtocol = "https";
  private final String canonicalUrl = "https://my-subdomain-1.my-domain.com/";
  private final String clientHost = "my-subdomain-2.my-domain.com";
  private final String sourceHostWithCanonicalUrl =
      "my-subdomain-1.my-domain.com, my-subdomain-2.my-domain.com";
  private final String sourceHostWithNoCanonicalUrl =
      "my-subdomain-2.my-domain.com, my-subdomain-3.my-domain.com";
  private final String emptyString = "      ";

  @Test
  public void shouldGoNextInChainWhenXForwardedHostHeaderNotPresent()
      throws IOException, ServletException {
    LoginOAuthRedirectionFilter loginOAuthRedirection =
        new LoginOAuthRedirectionFilter(canonicalUrl);
    loginOAuthRedirection.doFilter(httpServletRequest, httpServletResponse, filterChain);
    verify(httpServletRequest).getHeader(HttpHeaders.X_FORWARDED_HOST);
    verify(filterChain).doFilter(httpServletRequest, httpServletResponse);
  }

  @Test
  public void shouldGoNextInChainWhenXForwardedHostHeaderDefinedWithEmptyString()
      throws IOException, ServletException {
    when(httpServletRequest.getHeader(HttpHeaders.X_FORWARDED_HOST)).thenReturn(emptyString);
    LoginOAuthRedirectionFilter loginOAuthRedirection =
        new LoginOAuthRedirectionFilter(canonicalUrl);
    loginOAuthRedirection.doFilter(httpServletRequest, httpServletResponse, filterChain);
    verify(httpServletRequest).getHeader(HttpHeaders.X_FORWARDED_HOST);
    verify(filterChain).doFilter(httpServletRequest, httpServletResponse);
  }

  @Test
  public void shouldGoNextInChainWhenSourceDomainAndCanonicalWebDomainAreTheSame()
      throws IOException, ServletException {
    when(httpServletRequest.getHeader(HttpHeaders.X_FORWARDED_HOST))
        .thenReturn(sourceHostWithCanonicalUrl);
    LoginOAuthRedirectionFilter loginOAuthRedirection =
        new LoginOAuthRedirectionFilter(canonicalUrl);
    loginOAuthRedirection.doFilter(httpServletRequest, httpServletResponse, filterChain);
    verify(httpServletRequest).getHeader(HttpHeaders.X_FORWARDED_HOST);
    verify(filterChain).doFilter(httpServletRequest, httpServletResponse);
  }

  @Test
  public void shouldGoNextInChainWhenSourceDomainAndForwardedProtoAndPortHeadersNotDefined()
      throws IOException, ServletException {
    when(httpServletRequest.getHeader(HttpHeaders.X_FORWARDED_HOST))
        .thenReturn(sourceHostWithNoCanonicalUrl);
    LoginOAuthRedirectionFilter loginOAuthRedirection =
        new LoginOAuthRedirectionFilter(canonicalUrl);
    loginOAuthRedirection.doFilter(httpServletRequest, httpServletResponse, filterChain);
    verify(httpServletRequest).getHeader(HttpHeaders.X_FORWARDED_HOST);
    verify(httpServletRequest).getHeader(HttpHeaders.X_FORWARDED_PORT);
    verify(httpServletRequest).getHeader(HttpHeaders.X_FORWARDED_PROTO);
    verify(filterChain).doFilter(httpServletRequest, httpServletResponse);
  }

  @Test
  public void
      shouldRedirectToLoginWhenAllForwardedHeadersArePresentAndForwardedHostIsDifferentThanCanonicalWebHost()
          throws IOException, ServletException {
    when(httpServletRequest.getHeader(HttpHeaders.X_FORWARDED_HOST))
        .thenReturn(sourceHostWithNoCanonicalUrl);
    when(httpServletRequest.getHeader(HttpHeaders.X_FORWARDED_PORT)).thenReturn(sourceSSLPort);
    when(httpServletRequest.getHeader(HttpHeaders.X_FORWARDED_PROTO))
        .thenReturn(sourceHttpsProtocol);
    LoginOAuthRedirectionFilter loginOAuthRedirection =
        new LoginOAuthRedirectionFilter(canonicalUrl);
    loginOAuthRedirection.doFilter(httpServletRequest, httpServletResponse, filterChain);
    verify(httpServletRequest).getHeader(HttpHeaders.X_FORWARDED_HOST);
    verify(httpServletRequest).getHeader(HttpHeaders.X_FORWARDED_PORT);
    verify(httpServletRequest).getHeader(HttpHeaders.X_FORWARDED_PROTO);
    String finalRedirectUrlEncoded =
        URLEncoder.encode(
            sourceHttpsProtocol + "://" + clientHost + ":" + sourceSSLPort + "/", "UTF-8");
    verify(httpServletResponse)
        .sendRedirect(canonicalUrl + "login?final_redirect_url=" + finalRedirectUrlEncoded);
  }
}
