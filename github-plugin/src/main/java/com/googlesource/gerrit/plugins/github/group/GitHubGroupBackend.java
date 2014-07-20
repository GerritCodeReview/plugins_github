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

import static com.google.common.base.Preconditions.checkArgument;
import static com.googlesource.gerrit.plugins.github.group.GitHubOrganisationGroup.NAME_PREFIX;
import static com.googlesource.gerrit.plugins.github.group.GitHubOrganisationGroup.UUID_PREFIX;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.gerrit.common.data.GroupDescription.Basic;
import com.google.gerrit.common.data.GroupReference;
import com.google.gerrit.reviewdb.client.AccountGroup.UUID;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.GroupBackend;
import com.google.gerrit.server.account.GroupMembership;
import com.google.gerrit.server.project.ProjectControl;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.github.oauth.GitHubLogin;
import com.googlesource.gerrit.plugins.github.oauth.UserScopedProvider;

public class GitHubGroupBackend implements GroupBackend {
  private static final Logger log = LoggerFactory
      .getLogger(GitHubGroupBackend.class);
  private final GitHubGroupMembership.Factory ghMembershipProvider;
  private final GitHubOrganisationsCache ghOrganisationCache;

  @Inject
  public GitHubGroupBackend(
      UserScopedProvider<GitHubLogin> ghLogin,
      GitHubGroupMembership.Factory ghMembershipProvider,
      GitHubOrganisationsCache ghOrganisationCache) {
    this.ghMembershipProvider = ghMembershipProvider;
    this.ghOrganisationCache = ghOrganisationCache;
  }

  @Override
  public boolean handles(UUID uuid) {
    return uuid.get().startsWith(UUID_PREFIX);
  }

  @Override
  public Basic get(UUID uuid) {
    checkArgument(handles(uuid), "{} is not a valid GitHub Group UUID",
        uuid.get());
    return GitHubOrganisationGroup.fromUUID(uuid);
  }

  @Override
  public Collection<GroupReference> suggest(String name, ProjectControl project) {
    if (!name.startsWith(NAME_PREFIX)) {
      return Collections.emptyList();
    }
    String orgNamePrefix = name.substring(NAME_PREFIX.length());
    return listByPrefix(orgNamePrefix);
  }

  public Set<GroupReference> listByPrefix(String orgNamePrefix) {
    try {
      log.debug("Listing user's organisations starting with '{}'",
          orgNamePrefix);

      String orgNamePrefixLowercase = orgNamePrefix.toLowerCase();
      Set<String> ghOrgs = ghOrganisationCache.getOrganisationsForCurrentUser();
      log.debug("Full list of user's organisations: {}", ghOrgs);

      Builder<GroupReference> orgGroups =
          new ImmutableSet.Builder<GroupReference>();
      for (String ghOrg : ghOrgs) {
        if (ghOrg.toLowerCase().startsWith(orgNamePrefixLowercase)) {
          orgGroups.add(GitHubOrganisationGroup.groupReference(ghOrg));
        }
      }
      return orgGroups.build();
    } catch (ExecutionException e) {
      log.warn("Cannot get GitHub organisations matching '" + orgNamePrefix
          + "'", e);
    }

    return Collections.emptySet();
  }

  @Override
  public GroupMembership membershipsOf(IdentifiedUser user) {
    return ghMembershipProvider.get(user.getUserName());
  }
}
