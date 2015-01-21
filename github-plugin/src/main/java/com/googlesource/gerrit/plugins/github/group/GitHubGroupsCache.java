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

import org.kohsuke.github.GHTeam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@Singleton
public class GitHubGroupsCache {
  private static final Logger log = LoggerFactory
      .getLogger(GitHubGroupsCache.class);
  private static final String ORGS_CACHE_NAME = "groups";
  protected static final long GROUPS_CACHE_TTL_MINS = 60;
  public static final String EVERYONE_TEAM_NAME = "Everyone";

  public static class OrganisationLoader extends
      CacheLoader<String, Multimap<String, String>> {
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

      try {
        loadOrganisationsAndTeams(username, orgsTeams, ghLogin);
      } catch (FileNotFoundException teamsNotFound) {
        log.warn(
            "Cannot access teams for user '{}': falling back to list of public organisations",
            username);
        loadOrganisations(username, orgsTeams, ghLogin);
      }

      log.debug("GitHub user '{}' belongs to: {}", username, orgsTeams);
      return orgsTeams;
    }

    private void loadOrganisationsAndTeams(String username, Multimap<String, String> orgsTeams,
        GitHubLogin ghLogin) throws IOException {
      log.debug("Getting list of organisations/teams for user '{}'", username);
      Map<String, Set<GHTeam>> myOrganisationsLogins =
          ghLogin.getHub().getMyTeams();
      for (Entry<String, Set<GHTeam>> teamsOrg : myOrganisationsLogins
          .entrySet()) {
        orgsTeams.put(teamsOrg.getKey(), EVERYONE_TEAM_NAME);
        for (GHTeam team : teamsOrg.getValue()) {
          orgsTeams.put(teamsOrg.getKey(), team.getName());
        }
      }
    }

    private void loadOrganisations(String username,
        Multimap<String, String> orgsTeams, GitHubLogin ghLogin) throws IOException {
      log.debug("Getting list of public organisations for user '{}'", username);
      Set<String> organisations = ghLogin.getMyOrganisationsLogins();
      for (String org : organisations) {
        orgsTeams.put(org, EVERYONE_TEAM_NAME);
      }
    }
  }

  public static Module module() {
    return new CacheModule() {
      @Override
      protected void configure() {
        cache(ORGS_CACHE_NAME, String.class,
            new TypeLiteral<Multimap<String, String>>() {}).expireAfterWrite(
            GROUPS_CACHE_TTL_MINS, MINUTES).loader(OrganisationLoader.class);
        bind(GitHubGroupsCache.class);
      }
    };
  }

  private final LoadingCache<String, Multimap<String, String>> orgTeamsByUsername;
  private final Provider<IdentifiedUser> userProvider;

  @Inject
  GitHubGroupsCache(
      @Named(ORGS_CACHE_NAME) LoadingCache<String, Multimap<String, String>> byUsername,
      Provider<IdentifiedUser> userProvider) {
    this.orgTeamsByUsername = byUsername;
    this.userProvider = userProvider;
  }

  Set<String> getOrganizationsForUser(String username) {
    try {
      return orgTeamsByUsername.get(username).keySet();
    } catch (ExecutionException e) {
      log.warn("Cannot get GitHub organisations for user '" + username + "'", e);
      return Collections.emptySet();
    }
  }

  Set<String> getOrganizationsForCurrentUser() throws ExecutionException {
    return orgTeamsByUsername.get(userProvider.get().getUserName()).keySet();
  }

  Set<String> getTeamsForUser(String organizationName, String username) {
    try {
      return new ImmutableSet.Builder<String>().addAll(
          orgTeamsByUsername.get(username).get(organizationName)).build();
    } catch (ExecutionException e) {
      log.warn("Cannot get Teams membership for organisation '"
          + organizationName + "' and user '" + username + "'", e);
      return Collections.emptySet();
    }
  }

  Set<String> getTeamsForCurrentUser(String organizationName) {
    return getTeamsForUser(organizationName, userProvider.get().getUserName());
  }

  public Set<UUID> getGroupsForUser(String username) {
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
