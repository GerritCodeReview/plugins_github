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
package com.googlesource.gerrit.plugins.github.filters;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.github.oauth.GitHubLogin;
import com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig;
import com.googlesource.gerrit.plugins.github.oauth.OAuthProtocol;
import com.googlesource.gerrit.plugins.github.oauth.OAuthProtocol.Scope;
import com.googlesource.gerrit.plugins.github.oauth.ScopedProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Singleton
public class GitHubOAuthFilter implements Filter {
  private Logger LOG = LoggerFactory.getLogger(GitHubOAuthFilter.class);

  private static final List<String> whiteList = Arrays.asList(".png", ".jpg",
      ".js", ".css");

  private final ScopedProvider<GitHubLogin> loginProvider;
  private final Scope[] authScopes;
  private final OAuthProtocol oauth;
  private final GitHubOAuthConfig config;

  @Inject
  public GitHubOAuthFilter(ScopedProvider<GitHubLogin> loginProvider,
      GitHubOAuthConfig githubOAuthConfig, OAuthProtocol oauth) {
    this.loginProvider = loginProvider;
    this.authScopes = githubOAuthConfig.getDefaultScopes();
    this.oauth = oauth;
    this.config = githubOAuthConfig;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response,
      FilterChain chain) throws IOException, ServletException {
    GitHubLogin hubLogin = loginProvider.get((HttpServletRequest) request);
    LOG.debug("GitHub login: " + hubLogin);
    if (!hubLogin.isLoggedIn() && !isWhiteListed(request)) {
      hubLogin.login((HttpServletRequest) request,
          (HttpServletResponse) response, oauth, authScopes);
      return;
    } else {
      chain.doFilter(request, response);
    }
  }

  private boolean isWhiteListed(ServletRequest request) {
    String requestUri = ((HttpServletRequest) request).getRequestURI();
    for (String suffix : whiteList) {
      if (requestUri.endsWith(suffix)) {
        return true;
      }
    }
    return config.scopeSelectionUrl.endsWith(requestUri);
  }

  @Override
  public void destroy() {
  }

}
