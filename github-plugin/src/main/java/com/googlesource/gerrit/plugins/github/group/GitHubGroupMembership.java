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

import com.google.common.collect.ImmutableSet;
import com.google.gerrit.reviewdb.client.AccountGroup.UUID;
import com.google.gerrit.server.account.GroupMembership;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import java.util.Set;

public class GitHubGroupMembership implements GroupMembership {
  private final Set<UUID> groups;

  public interface Factory {
    GitHubGroupMembership get(@Assisted String username);
  }

  @Inject
  GitHubGroupMembership(GitHubGroupsCache ghOrganisationCache,
      @Assisted String username) {
    this.groups =
        new ImmutableSet.Builder<UUID>().addAll(
            ghOrganisationCache.getGroupsForUser(username)).build();
  }

  @Override
  public boolean contains(UUID groupId) {
    return groups.contains(groupId);
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
    return groups;
  }
}
