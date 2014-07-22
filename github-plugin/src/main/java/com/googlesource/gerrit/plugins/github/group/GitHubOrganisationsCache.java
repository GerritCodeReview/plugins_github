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
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHTeam;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.PagedIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMultimap;
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
  private static final String ORGS_CACHE_NAME = "organisations";
  private static final String TEAMS_CACHE_NAME = "teams";
  protected static final long GROUPS_CACHE_TTL_MINS = 15;

  public static class OrganisationLoader extends
      CacheLoader<String, Set<String>> {
    private static final Logger log = LoggerFactory
        .getLogger(OrganisationLoader.class);
    private final UserScopedProvider<GitHubLogin> ghLoginProvider;

    @Inject
    public OrganisationLoader(UserScopedProvider<GitHubLogin> ghLoginProvider) {
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
      log.debug("GitHub user '{}' belongs to: {}", username,
          myOrganisationsLogins);
      return myOrganisationsLogins;
    }
  }

  public static class TeamLoader extends
      CacheLoader<String, Multimap<String, String>> {
    private static final Logger log = LoggerFactory
        .getLogger(OrganisationLoader.class);
    private final UserScopedProvider<GitHubLogin> ghLoginProvider;

    @Inject
    public TeamLoader(UserScopedProvider<GitHubLogin> ghLoginProvider) {
      this.ghLoginProvider = ghLoginProvider;
    }

    @Override
    public Multimap<String, String> load(String orgName) throws Exception {
      ImmutableMultimap.Builder<String, String> teamsBuilder =
          new ImmutableMultimap.Builder<>();

      GitHubLogin ghLogin = ghLoginProvider.get();
      if (ghLogin == null) {
        log.warn("Cannot login to GitHub on behalf of current user");
        return teamsBuilder.build();
      }

      log.debug("Accessing GitHub organisation {}", orgName);
      GHOrganization ghOrg = ghLogin.getHub().getOrganization(orgName);

      log.debug("Getting list of teams for organisation '{}'", orgName);
      PagedIterable<GHTeam> ghTeams = ghOrg.listTeams();

      for (GHTeam ghTeam : ghTeams) {
        Set<String> teamMembers = new HashSet<>();
        log.debug("Getting list of members for team '{}'", ghTeam.getName());
        Set<GHUser> ghMembers = ghTeam.getMembers();
        for (GHUser ghUser : ghMembers) {
          teamMembers.add(ghUser.getLogin());
        }
        teamsBuilder.putAll(ghTeam.getName(), teamMembers);
      }
      ImmutableMultimap<String, String> teams = teamsBuilder.build();
      log.debug("GitHub organisation '{}' has teams/members {}", orgName, teams);
      return teams;
    }
  }

  public static Module module() {
    return new CacheModule() {
      @Override
      protected void configure() {
        cache(ORGS_CACHE_NAME, String.class, new TypeLiteral<Set<String>>() {})
            .expireAfterWrite(GROUPS_CACHE_TTL_MINS, MINUTES).loader(
                OrganisationLoader.class);
        cache(TEAMS_CACHE_NAME, String.class,
            new TypeLiteral<Multimap<String, String>>() {}).expireAfterWrite(
            GROUPS_CACHE_TTL_MINS, MINUTES).loader(TeamLoader.class);
        bind(GitHubOrganisationsCache.class);
      }
    };
  }

  private final LoadingCache<String, Set<String>> orgsByUsername;
  private final LoadingCache<String, Multimap<String, String>> teamsByOrgName;
  private final Provider<IdentifiedUser> userProvider;

  @Inject
  public GitHubOrganisationsCache(
      @Named(ORGS_CACHE_NAME) LoadingCache<String, Set<String>> byUsername,
      @Named(TEAMS_CACHE_NAME) LoadingCache<String, Multimap<String, String>> teamsByOrgName,
      Provider<IdentifiedUser> userProvider) {
    this.orgsByUsername = byUsername;
    this.userProvider = userProvider;
    this.teamsByOrgName = teamsByOrgName;
  }

  public Set<String> getOrgsForUser(String username) {
    try {
      return orgsByUsername.get(username);
    } catch (ExecutionException e) {
      log.warn("Cannot get GitHub organisations for user '" + username + "'", e);
      return Collections.emptySet();
    }
  }

  public Set<String> getOrgsForCurrentUser() throws ExecutionException {
    return orgsByUsername.get(userProvider.get().getUserName());
  }

  public Set<UUID> getOrgsUUIDForUser(String username) {
    Set<String> ghOrgsLogins = getOrgsForUser(username);
    log.debug("GitHub user '{}' belongs to: {}", username, ghOrgsLogins);
    ImmutableSet.Builder<UUID> groupSet = new ImmutableSet.Builder<UUID>();
    for (String ghOrg : ghOrgsLogins) {
      groupSet.add(GitHubOrganisationGroup.uuid(ghOrg));
    }
    return groupSet.build();
  }

  public Set<String> getTeamsForCurrentUser() {
    return Collections.emptySet();
  }

  public Set<String> getTeamsForOrg(String orgName) {
    try {
      return teamsByOrgName.get(orgName).keySet();
    } catch (ExecutionException e) {
      log.warn("Cannot get Teams for organisation '" + orgName + "'", e);
      return Collections.emptySet();
    }
  }


  public Set<String> getTeamsForOrgUser(String orgName, String username) {
    ImmutableSet.Builder<String> teamsMembership = new ImmutableSet.Builder<>();
    try {
      for (Entry<String, String> teamMember : teamsByOrgName.get(orgName)
          .entries()) {
        if (teamMember.getValue().equals(username)) {
          teamsMembership.add(teamMember.getKey());
        }
      }
      return teamsMembership.build();
    } catch (ExecutionException e) {
      log.warn("Cannot get Teams membership for organisation '" + orgName
          + "' and user '" + username + "'", e);
      return Collections.emptySet();
    }
  }
}
