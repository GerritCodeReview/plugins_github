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

import com.google.gerrit.common.Nullable;
import com.google.gerrit.common.data.GroupReference;
import com.google.gerrit.reviewdb.client.AccountGroup;
import com.google.gerrit.reviewdb.client.AccountGroup.UUID;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

public class GitHubTeamGroup extends GitHubGroup {
  public interface Factory {
    GitHubTeamGroup get(
        @Assisted GitHubOrganisationGroup orgGroup,
        @Assisted String teamName,
        @Nullable String teamUrl);
  }

  private final GitHubOrganisationGroup orgGroup;
  private final String teamName;

  @Inject
  GitHubTeamGroup(
      @Assisted GitHubOrganisationGroup orgGroup,
      @Assisted String teamName,
      @Nullable String teamUrl) {
    super(uuid(orgGroup.getGroupUUID(), teamName), teamUrl);
    this.orgGroup = orgGroup;
    this.teamName = teamName;
  }

  @Override
  public String getName() {
    return orgGroup.getName() + "/" + teamName;
  }

  public static UUID uuid(UUID orgUUID, String teamName) {
    return new AccountGroup.UUID(orgUUID.get() + "/" + teamName);
  }

  public static GroupReference groupReference(GroupReference orgReference, String teamName) {
    return new GroupReference(
        uuid(orgReference.getUUID(), teamName), orgReference.getName() + "/" + teamName);
  }
}
