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

import static com.google.common.base.Preconditions.checkArgument;
import static com.googlesource.gerrit.plugins.github.GitHubOrganisationGroup.NAME_PREFIX;
import static com.googlesource.gerrit.plugins.github.GitHubOrganisationGroup.UUID_PREFIX;

import java.util.Collection;
import java.util.Collections;

import com.google.gerrit.common.data.GroupDescription.Basic;
import com.google.gerrit.common.data.GroupReference;
import com.google.gerrit.reviewdb.client.AccountGroup.UUID;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.GroupBackend;
import com.google.gerrit.server.account.GroupMembership;
import com.google.gerrit.server.project.ProjectControl;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.github.oauth.GitHubAnonymousLogin;

public class GitHubGroupBackend implements GroupBackend {
  private static final int NAME_SUGGESTION_LENGTH = 3;

  private final GitHubOrganisationGroup.Factory ghOrganisationFactory;
  private final GitHubAnonymousLogin ghLogin;

  @Inject
  public GitHubGroupBackend(
      GitHubOrganisationGroup.Factory ghOrganisationFactory,
      GitHubAnonymousLogin ghLogin) {
    this.ghOrganisationFactory = ghOrganisationFactory;
    this.ghLogin = ghLogin;
  }

  @Override
  public boolean handles(UUID uuid) {
    return uuid.get().startsWith(UUID_PREFIX);
  }

  @Override
  public Basic get(UUID uuid) {
    checkArgument(handles(uuid), "{} is not a valid GitHub Group UUID",
        uuid.get());
    String gitHubOrganisation = uuid.get().substring(UUID_PREFIX.length());
    return ghOrganisationFactory.get(gitHubOrganisation, null);
  }

  @Override
  public Collection<GroupReference> suggest(String name, ProjectControl project) {
    if (name.length() < (NAME_PREFIX.length() + NAME_SUGGESTION_LENGTH)
        || !name.startsWith(NAME_PREFIX)) {
      return Collections.emptyList();
    }
    String orgNamePrefix = name.substring(NAME_PREFIX.length());
    return GitHubOrganisationGroup.listByPrefix(ghLogin, orgNamePrefix);
  }

  @Override
  public GroupMembership membershipsOf(IdentifiedUser user) {
    return new GitHubGroupMembership(ghLogin, user.getUserName());
  }
}
