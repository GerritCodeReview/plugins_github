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

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.gerrit.reviewdb.client.AccountExternalId;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.AccountCache;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.github.oauth.GitHubLogin;
import com.googlesource.gerrit.plugins.github.oauth.OAuthFilter;
import com.googlesource.gerrit.plugins.github.oauth.OAuthProtocol.AccessToken;
import com.googlesource.gerrit.plugins.github.oauth.OAuthWebFilter;
import com.googlesource.gerrit.plugins.github.oauth.ScopedProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;

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
  private final Provider<CurrentUser> userProvider;
  private final AccountCache accountCache;

  @Inject
  public GitHubOAuthFilter(ScopedProvider<GitHubLogin> loginProvider,
      Provider<CurrentUser> userProvider,
      AccountCache accountCache) {
    this.loginProvider = loginProvider;
    this.userProvider = userProvider;
    this.accountCache = accountCache;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response,
      FilterChain chain) throws IOException, ServletException {
    GitHubLogin hubLogin = loginProvider.get((HttpServletRequest) request);
    LOG.debug("GitHub login: " + hubLogin);
    CurrentUser user = userProvider.get();
    if (!hubLogin.isLoggedIn()
        && !OAuthFilter.skipOAuth((HttpServletRequest) request)
        && user.isIdentifiedUser()) {
      AccountExternalId gitHubExtId = getGitHubExternalId(user);

      String oauthToken =
          gitHubExtId.getSchemeRest()
              .substring(OAuthWebFilter.GITHUB_EXT_ID.length());
      hubLogin.login(new AccessToken(oauthToken));
    }

    chain.doFilter(request, response);
  }

  private AccountExternalId getGitHubExternalId(CurrentUser user) {
    Collection<AccountExternalId> accountExtIds =
        accountCache.get(((IdentifiedUser) user).getAccountId())
            .getExternalIds();
    Collection<AccountExternalId> gitHubExtId =
        Collections2.filter(accountExtIds,
            new Predicate<AccountExternalId>() {
              @Override
              public boolean apply(AccountExternalId externalId) {
                return externalId.isScheme(AccountExternalId.SCHEME_EXTERNAL)
                    && externalId.getSchemeRest().startsWith(
                        OAuthWebFilter.GITHUB_EXT_ID);
              }
            });

    if (gitHubExtId.isEmpty()) {
      throw new IllegalStateException("Current Gerrit user "
          + user.getUserName() + " has no GitHub OAuth external ID");
    }
    return gitHubExtId.iterator().next();
  }

  @Override
  public void destroy() {
  }

}