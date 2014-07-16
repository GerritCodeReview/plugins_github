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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHPersonSet;
import org.kohsuke.github.GHUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.gerrit.common.Nullable;
import com.google.gerrit.common.data.GroupDescription.Basic;
import com.google.gerrit.common.data.GroupReference;
import com.google.gerrit.reviewdb.client.AccountGroup;
import com.google.gerrit.reviewdb.client.AccountGroup.UUID;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.googlesource.gerrit.plugins.github.oauth.GitHubAnonymousLogin;

public class GitHubOrganisationGroup implements Basic {
  public static final String UUID_PREFIX = "github:";
  public static final String NAME_PREFIX = "github/";

  public interface Factory {
    GitHubOrganisationGroup get(@Assisted("orgName") String orgName,
        @Assisted("orgUrl") @Nullable String orgUrl);
  }

  private static final Logger log = LoggerFactory
      .getLogger(GitHubOrganisationGroup.class);

  private final String orgName;
  private final UUID uuid;
  private final String url;

  @Inject
  GitHubOrganisationGroup(@Assisted("orgName") String orgName,
      @Assisted("orgUrl") @Nullable String orgUrl) {
    this.orgName = orgName;
    this.uuid = uuid(orgName);
    this.url = orgUrl;
  }

  @Override
  public String getEmailAddress() {
    return "";
  }

  @Override
  public UUID getGroupUUID() {
    return uuid;
  }

  @Override
  public String getName() {
    return NAME_PREFIX + orgName;
  }

  @Override
  public String getUrl() {
    return url;
  }

  public static GitHubOrganisationGroup fromUUID(UUID uuid) {
    checkArgument(uuid.get().startsWith(UUID_PREFIX), "Invalid GitHub UUID '"
        + uuid + "'");
    return new GitHubOrganisationGroup(uuid.get().substring(
        UUID_PREFIX.length()), null);
  }

  public static Set<GroupReference> listByPrefix(GitHubAnonymousLogin ghLogin,
      String orgNamePrefix) {
    if (ghLogin.loginAnonymously()) {
      try {
        GHOrganization ghOrg = ghLogin.getHub().getOrganization(orgNamePrefix);
        return new ImmutableSet.Builder<GroupReference>().add(
            new GroupReference(uuid(ghOrg.getLogin()), NAME_PREFIX + ghOrg.getLogin()))
            .build();
      } catch (FileNotFoundException fileNotFound) {
        log.debug("No GitHub organisation found for {}", orgNamePrefix);
      } catch (IOException e) {
        log.warn("Cannot get GitHub organisation matching '" + orgNamePrefix
            + "'", e);
      }
    }

    return Collections.emptySet();
  }

  public static Set<UUID> listByUser(GitHubAnonymousLogin ghLogin,
      String username) {
    if (ghLogin.loginAnonymously()) {
      try {
        GHUser ghUser = ghLogin.getHub().getUser(username);
        GHPersonSet<GHOrganization> ghOrgs = ghUser.getOrganizations();
        ImmutableSet.Builder<UUID> groupSet = new ImmutableSet.Builder<UUID>();
        for (GHOrganization ghOrg : ghOrgs) {
          groupSet.add(uuid(ghOrg.getLogin()));
        }
        return groupSet.build();
      } catch (FileNotFoundException fileNotFound) {
        log.debug("No GitHub organisation found for user '{}'", username);
      } catch (IOException e) {
        log.warn("Cannot get GitHub organisations for user '" + username + "'",
            e);
      }
    }

    return Collections.emptySet();
  }

  private static UUID uuid(String orgName) {
    return new AccountGroup.UUID(UUID_PREFIX + orgName);
  }
}
