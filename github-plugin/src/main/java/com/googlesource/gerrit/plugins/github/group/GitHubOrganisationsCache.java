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

package com.googlesource.gerrit.plugins.github.group;

import static java.util.concurrent.TimeUnit.MINUTES;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import com.google.gerrit.reviewdb.client.AccountGroup.UUID;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.cache.CacheModule;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import com.googlesource.gerrit.plugins.github.oauth.GitHubLogin;
import com.googlesource.gerrit.plugins.github.oauth.UserScopedProvider;

@Singleton
public class GitHubOrganisationsCache {
  private static final Logger log = LoggerFactory
      .getLogger(GitHubOrganisationsCache.class);
  private static final String CACHE_NAME = "organisations";
  protected static final long GROUPS_CACHE_TTL_MINS = 15;

  public static class Loader extends CacheLoader<String, Set<String>> {
    private static final Logger log = LoggerFactory.getLogger(Loader.class);
    private final UserScopedProvider<GitHubLogin> ghLoginProvider;

    @Inject
    public Loader(UserScopedProvider<GitHubLogin> ghLoginProvider) {
      this.ghLoginProvider = ghLoginProvider;
    }

    @Override
    public Set<String> load(String username) throws Exception {
      GitHubLogin ghLogin = ghLoginProvider.get(username);
      if (ghLogin == null) {
        log.warn("Cannot login to GitHub on behalf of '{}'", username);
        return Collections.emptySet();
      }

      log.debug("Getting list of organisations for user '{}'", username);
      Set<String> myOrganisationsLogins = ghLogin.getMyOrganisationsLogins();
      log.debug("GitHub user '{}' belongs to: {}", username, myOrganisationsLogins);
      return myOrganisationsLogins;
    }
  }

  public static Module module() {
    return new CacheModule() {
      @Override
      protected void configure() {
        cache(CACHE_NAME, String.class, new TypeLiteral<Set<String>>() {})
            .expireAfterWrite(GROUPS_CACHE_TTL_MINS, MINUTES)
            .loader(Loader.class);
        bind(GitHubOrganisationsCache.class);
      }
    };
  }

  private LoadingCache<String, Set<String>> byUsername;
  private final Provider<IdentifiedUser> userProvider;

  @Inject
  public GitHubOrganisationsCache(
      @Named(CACHE_NAME) LoadingCache<String, Set<String>> byUsername,
      Provider<IdentifiedUser> userProvider) {
    this.byUsername = byUsername;
    this.userProvider = userProvider;
  }

  public Set<String> getOrganisationsForUsername(String username)
      throws ExecutionException {
    return byUsername.get(username);
  }
  
  public Set<String> getOrganisationsForCurrentUser()
      throws ExecutionException {
    return byUsername.get(userProvider.get().getUserName());
  }

  public Set<UUID> getOrganisationsGroupsForUsername(String username) {
    try {
      Set<String> ghOrgsLogins = getOrganisationsForUsername(username);
      log.debug("GitHub user '{}' belongs to: {}", username, ghOrgsLogins);
      ImmutableSet.Builder<UUID> groupSet = new ImmutableSet.Builder<UUID>();
      for (String ghOrg : ghOrgsLogins) {
        groupSet.add(GitHubOrganisationGroup.uuid(ghOrg));
      }
      return groupSet.build();
    } catch (ExecutionException e) {
      log.warn("Cannot get GitHub organisations for user '" + username + "'", e);
    }
    return Collections.emptySet();
  }
}
