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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

// TODO fix the tests
@RunWith(MockitoJUnitRunner.class)
public class LoginOAuthRedirectionFilterTest {
  @Mock private HttpServletRequest httpServletRequest;
  @Mock private HttpServletResponse httpServletResponse;
  @Mock private FilterChain filterChain;

  private final String canonicalUrl = "https://my-subdomain-1.my-domain.com/";

  @Test
  @Ignore
  public void shouldGoNextInChainWhenXForwardedHostHeaderIsNotPresent()
      throws IOException, ServletException {
    LoginOAuthRedirectionFilter loginOAuthRedirection =
        new LoginOAuthRedirectionFilter(canonicalUrl);
    loginOAuthRedirection.doFilter(httpServletRequest, httpServletResponse, filterChain);
    verify(httpServletRequest).getHeader(LoginOAuthRedirectionFilter.X_FORWARDED_HOST_HTTP_HEADER);
    verify(filterChain).doFilter(httpServletRequest, httpServletResponse);
  }

  // TODO IMPROVE TEST NAME
  @Test
  @Ignore
  public void shouldRedirectToLoginWhenXForwardedHostHeaderIsTheSameAsCanonicalWebUrlHost()
      throws IOException, ServletException {
    when(httpServletRequest.getHeader(LoginOAuthRedirectionFilter.X_FORWARDED_HOST_HTTP_HEADER))
        .thenReturn("my-subdomain-1.my-domain.com");
    LoginOAuthRedirectionFilter loginOAuthRedirection =
        new LoginOAuthRedirectionFilter(canonicalUrl);
    loginOAuthRedirection.doFilter(httpServletRequest, httpServletResponse, filterChain);
    verify(httpServletRequest).getHeader(LoginOAuthRedirectionFilter.X_FORWARDED_HOST_HTTP_HEADER);
    verify(httpServletResponse).sendRedirect("/login");
  }

  // TODO IMPROVE TEST NAME
  @Test
  @Ignore
  public void
      shouldRedirectToLoginToCanonicalWebWithQueryParameterWhenXForwardedHostHeaderIsNotTheSameAsCanonicalWebUrlHost()
          throws IOException, ServletException {
    when(httpServletRequest.getHeader(LoginOAuthRedirectionFilter.X_FORWARDED_HOST_HTTP_HEADER))
        .thenReturn("my-subdomain-2.my-domain.com");
    LoginOAuthRedirectionFilter loginOAuthRedirection =
        new LoginOAuthRedirectionFilter(canonicalUrl);
    loginOAuthRedirection.doFilter(httpServletRequest, httpServletResponse, filterChain);
    verify(httpServletRequest).getHeader(LoginOAuthRedirectionFilter.X_FORWARDED_HOST_HTTP_HEADER);
    String finalRedirectUrlEncoded =
        URLEncoder.encode("https://my-subdomain-2.my-domain.com", "UTF-8");
    verify(httpServletResponse)
        .sendRedirect(canonicalUrl + "login?final_redirect_url=" + finalRedirectUrlEncoded);
  }
}