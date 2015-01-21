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
import com.googlesource.gerrit.plugins.github.oauth.OAuthProtocol.Scope;
import com.googlesource.gerrit.plugins.github.oauth.ScopedProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

@Singleton
public class GitHubOAuthFilter implements Filter {
  private Logger LOG = LoggerFactory.getLogger(GitHubOAuthFilter.class);

  private final ScopedProvider<GitHubLogin> loginProvider;
  private final Scope[] authScopes;

  @Inject
  public GitHubOAuthFilter(final ScopedProvider<GitHubLogin> loginProvider,
      final GitHubOAuthConfig githubOAuthConfig) {
    this.loginProvider = loginProvider;
    this.authScopes = githubOAuthConfig.getDefaultScopes();
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response,
      FilterChain chain) throws IOException, ServletException {
    GitHubLogin hubLogin = loginProvider.get((HttpServletRequest) request);
    LOG.debug("GitHub login: " + hubLogin);
    if (!hubLogin.isLoggedIn()) {
      hubLogin.login(request, response, authScopes);
      return;
    } else {
      chain.doFilter(request, response);
    }
  }

  @Override
  public void destroy() {
  }

}
