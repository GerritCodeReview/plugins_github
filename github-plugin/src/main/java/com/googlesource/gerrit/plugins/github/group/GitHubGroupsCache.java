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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.kohsuke.github.GHTeam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
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
  private static final String ORGS_CACHE_NAME = "org-teams";
  protected static final long GROUPS_CACHE_TTL_MINS = 15;

  public static class OrganisationLoader extends
      CacheLoader<String, Multimap<String,String>> {
    private static final Logger log = LoggerFactory
        .getLogger(OrganisationLoader.class);
    private final UserScopedProvider<GitHubLogin> ghLoginProvider;

    @Inject
    public OrganisationLoader(UserScopedProvider<GitHubLogin> ghLoginProvider) {
      this.ghLoginProvider = ghLoginProvider;
    }

    @Override
    public Multimap<String, String> load(String username) throws Exception {
      Multimap<String, String> orgsTeams = HashMultimap.create();
      GitHubLogin ghLogin = ghLoginProvider.get(username);
      if (ghLogin == null) {
        log.warn("Cannot login to GitHub on behalf of '{}'", username);
        return orgsTeams;
      }

      log.debug("Getting list of organisations/teams for user '{}'", username);
      Map<String, Set<GHTeam>> myOrganisationsLogins =
          ghLogin.getHub().getMyTeams();
      for (Entry<String, Set<GHTeam>> teamsOrg : myOrganisationsLogins
          .entrySet()) {
        for (GHTeam team : teamsOrg.getValue()) {
          orgsTeams.put(teamsOrg.getKey(), team.getName());
        }
      }
      log.debug("GitHub user '{}' belongs to: {}", username, orgsTeams);
      return orgsTeams;
    }
  }

  public static Module module() {
    return new CacheModule() {
      @Override
      protected void configure() {
        cache(ORGS_CACHE_NAME, String.class, new TypeLiteral<Multimap<String,String>>() {})
            .expireAfterWrite(GROUPS_CACHE_TTL_MINS, MINUTES).loader(
                OrganisationLoader.class);
        bind(GitHubOrganisationsCache.class);
      }
    };
  }

  private final LoadingCache<String, Multimap<String,String>> orgTeamsByUsername;
  private final Provider<IdentifiedUser> userProvider;

  @Inject
  public GitHubOrganisationsCache(
      @Named(ORGS_CACHE_NAME) LoadingCache<String, Multimap<String,String>> byUsername,
      Provider<IdentifiedUser> userProvider) {
    this.orgTeamsByUsername = byUsername;
    this.userProvider = userProvider;
  }

  public Set<String> getOrganizationsForUser(String username) {
    try {
      return orgTeamsByUsername.get(username).keySet();
    } catch (ExecutionException e) {
      log.warn("Cannot get GitHub organisations for user '" + username + "'", e);
      return Collections.emptySet();
    }
  }

  public Set<String> getOrganizationsForCurrentUser() throws ExecutionException {
    return orgTeamsByUsername.get(userProvider.get().getUserName()).keySet();
  }

  public Set<String> getTeamsForUser(String organizationName, String username) {
    try {
      return new ImmutableSet.Builder<String>().addAll(
          orgTeamsByUsername.get(username).get(organizationName)).build();
    } catch (ExecutionException e) {
      log.warn("Cannot get Teams membership for organisation '" + organizationName
          + "' and user '" + username + "'", e);
      return Collections.emptySet();
    }
  }

  public Set<String> getTeamsForCurrentUser(String organizationName) {
    return getTeamsForUser(organizationName, userProvider.get().getUserName());
  }

  public Set<UUID> getAllGroupsForUser(String username) {
    ImmutableSet.Builder<UUID> groupsBuilder = new ImmutableSet.Builder<>();
      for (String org : getOrganizationsForUser(username)) {
        groupsBuilder.add(GitHubOrganisationGroup.uuid(org));

        for (String team : getTeamsForUser(org, username)) {
          groupsBuilder.add(GitHubTeamGroup.uuid(
              GitHubOrganisationGroup.uuid(org), team));
        }
      }
      return groupsBuilder.build();
  }
}
