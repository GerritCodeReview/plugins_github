// Copyright (C) 2014 The Android Open Source Project
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

import com.google.gerrit.common.Nullable;
import com.google.gerrit.reviewdb.client.AccountExternalId;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.AccountCache;
import com.google.gerrit.server.account.AccountState;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import com.googlesource.gerrit.plugins.github.oauth.OAuthProtocol.AccessToken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;

@Singleton
public class IdentifiedUserGitHubLoginProvider implements
    UserScopedProvider<GitHubLogin> {
  private static final Logger log = LoggerFactory.getLogger(IdentifiedUserGitHubLoginProvider.class);
  private static final String EXTERNAL_ID_PREFIX = "external:"
      + OAuthWebFilter.GITHUB_EXT_ID;

  private final Provider<IdentifiedUser> userProvider;
  private OAuthProtocol oauth;
  private GitHubOAuthConfig config;
  private AccountCache accountCache;

  @Inject
  public IdentifiedUserGitHubLoginProvider(
      final Provider<IdentifiedUser> identifiedUserProvider,
      final OAuthProtocol oauth, final GitHubOAuthConfig config,
      final AccountCache accountCache) {
    this.userProvider = identifiedUserProvider;
    this.oauth = oauth;
    this.config = config;
    this.accountCache = accountCache;
  }

  @Override
  public GitHubLogin get() {
    IdentifiedUser currentUser = userProvider.get();
    return get(currentUser.getUserName());
  }

  @Override
  @Nullable
  public GitHubLogin get(String username) {
    try {
      AccessToken accessToken = newAccessTokenFromUser(username);
      if (accessToken != null) {
        GitHubLogin login = new GitHubLogin(oauth, config);
        login.login(accessToken);
        return login;
      } else {
        return null;
      }
    } catch (IOException e) {
      log.error("Cannot login to GitHub as '" + username
          + "'", e);
      return null;
    }
  }

  private AccessToken newAccessTokenFromUser(String username) {
    AccountState account = accountCache.getByUsername(username);
    Collection<AccountExternalId> externalIds = account.getExternalIds();
    for (AccountExternalId accountExternalId : externalIds) {
      String key = accountExternalId.getKey().get();
      if (key.startsWith(EXTERNAL_ID_PREFIX)) {
        return new AccessToken(key.substring(EXTERNAL_ID_PREFIX.length()));
      }
    }

    return null;
  }
}
