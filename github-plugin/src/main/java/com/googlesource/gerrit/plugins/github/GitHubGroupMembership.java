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

package com.googlesource.gerrit.plugins.github;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.gerrit.reviewdb.client.AccountGroup.UUID;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.GroupMembership;
import com.googlesource.gerrit.plugins.github.oauth.GitHubAnonymousLogin;

public class GitHubGroupMembership implements GroupMembership {
  private final Set<UUID> groups;
  private final GitHubAnonymousLogin ghLogin;
  private final String username;

  public GitHubGroupMembership(GitHubAnonymousLogin ghLogin, String username) {
    this.username = username;
    this.ghLogin = ghLogin;
    this.groups = GitHubOrganisationGroup.listByUser(ghLogin, username);
  }

  @Override
  public boolean contains(UUID groupId) {
    return groups.contains(GitHubOrganisationGroup.fromUUID(groupId));
  }

  @Override
  public boolean containsAnyOf(Iterable<UUID> groupIds) {
    return !intersection(groupIds).isEmpty();
  }

  @Override
  public Set<UUID> intersection(Iterable<UUID> groupIds) {
    ImmutableSet.Builder<UUID> groups = new ImmutableSet.Builder<>();
    for (UUID uuid : groupIds) {
      if (contains(uuid)) {
        groups.add(uuid);
      }
    }
    return groups.build();
  }

  @Override
  public Set<UUID> getKnownGroups() {
    return new ImmutableSet.Builder<UUID>().addAll(
        GitHubOrganisationGroup.listByUser(ghLogin, username)).build();
  }
}
