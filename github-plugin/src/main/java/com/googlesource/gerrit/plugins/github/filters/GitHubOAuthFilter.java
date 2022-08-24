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
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.AccountCache;
import com.google.gerrit.server.account.externalids.ExternalId;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.github.oauth.GitHubLogin;
import com.googlesource.gerrit.plugins.github.oauth.IdentifiedUserGitHubLoginProvider;
import com.googlesource.gerrit.plugins.github.oauth.OAuthFilter;
import com.googlesource.gerrit.plugins.github.oauth.OAuthProtocol.AccessToken;
import com.googlesource.gerrit.plugins.github.oauth.OAuthTokenCipher;
import com.googlesource.gerrit.plugins.github.oauth.OAuthWebFilter;
import com.googlesource.gerrit.plugins.github.oauth.ScopedProvider;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.util.Collection;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class GitHubOAuthFilter implements Filter {
  private Logger LOG = LoggerFactory.getLogger(GitHubOAuthFilter.class);

  private final ScopedProvider<GitHubLogin> loginProvider;
  private final Provider<CurrentUser> userProvider;
  private final AccountCache accountCache;
  private final OAuthTokenCipher oAuthTokenCipher;

  @Inject
  public GitHubOAuthFilter(
      ScopedProvider<GitHubLogin> loginProvider,
      Provider<CurrentUser> userProvider,
      AccountCache accountCache,
      OAuthTokenCipher oAuthTokenCipher) {
    this.loginProvider = loginProvider;
    this.userProvider = userProvider;
    this.accountCache = accountCache;
    this.oAuthTokenCipher = oAuthTokenCipher;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {}

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    GitHubLogin hubLogin = loginProvider.get((HttpServletRequest) request);
    LOG.debug("GitHub login: " + hubLogin);
    CurrentUser user = userProvider.get();
    if (!hubLogin.isLoggedIn()
        && !OAuthFilter.skipOAuth((HttpServletRequest) request)
        && user.isIdentifiedUser()) {
      ExternalId gitHubExtId = getGitHubExternalId(user);

      String oauthToken =
          gitHubExtId
              .key()
              .get()
              .substring(
                  ExternalId.SCHEME_EXTERNAL.length() + OAuthWebFilter.GITHUB_EXT_ID.length() + 1);
      try {
        String decryptedToken = oAuthTokenCipher.decrypt(oauthToken);
        hubLogin.login(new AccessToken(decryptedToken));
      } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
        throw new IOException("Could not decrypt oauthToken", e);
      }
    }

    chain.doFilter(request, response);
  }

  private ExternalId getGitHubExternalId(CurrentUser user) {
    Collection<ExternalId> accountExtIds =
        accountCache.get(((IdentifiedUser) user).getAccountId()).get().externalIds();
    Collection<ExternalId> gitHubExtId =
        Collections2.filter(
            accountExtIds,
            new Predicate<ExternalId>() {
              @Override
              public boolean apply(ExternalId externalId) {
                return externalId
                    .key()
                    .get()
                    .startsWith(IdentifiedUserGitHubLoginProvider.EXTERNAL_ID_PREFIX);
              }
            });

    if (gitHubExtId.isEmpty()) {
      throw new IllegalStateException(
          "Current Gerrit user " + user.getUserName() + " has no GitHub OAuth external ID");
    }
    return gitHubExtId.iterator().next();
  }

  @Override
  public void destroy() {}
}
