// Copyright (C) 2013 The Android Open Source Project
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

import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.extensions.restapi.RestApiModule;
import com.google.gerrit.extensions.webui.TopMenu;
import com.google.gerrit.server.account.GroupBackend;
import com.google.gerrit.server.events.EventListener;
import com.google.gerrit.server.project.ProjectResource;
import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.googlesource.gerrit.plugins.github.group.GitHubGroupBackend;
import com.googlesource.gerrit.plugins.github.group.GitHubGroupMembership;
import com.googlesource.gerrit.plugins.github.group.GitHubGroupsCache;
import com.googlesource.gerrit.plugins.github.group.GitHubOrganisationGroup;
import com.googlesource.gerrit.plugins.github.oauth.GitHubLogin;
import com.googlesource.gerrit.plugins.github.oauth.IdentifiedUserGitHubLoginProvider;
import com.googlesource.gerrit.plugins.github.oauth.UserScopedProvider;
import com.googlesource.gerrit.plugins.github.replication.GerritGsonProvider;
import com.googlesource.gerrit.plugins.github.replication.ListProjectReplicationStatus;
import com.googlesource.gerrit.plugins.github.replication.ReplicationStatusFlatFile;
import com.googlesource.gerrit.plugins.github.replication.ReplicationStatusListener;
import com.googlesource.gerrit.plugins.github.replication.ReplicationStatusStore;

public class GuiceModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(new TypeLiteral<UserScopedProvider<GitHubLogin>>() {})
        .to(IdentifiedUserGitHubLoginProvider.class);

    install(GitHubGroupsCache.module());

    DynamicSet.bind(binder(), TopMenu.class).to(GitHubTopMenu.class);
    DynamicSet.bind(binder(), GroupBackend.class).to(GitHubGroupBackend.class);
    DynamicSet.bind(binder(), EventListener.class).to(ReplicationStatusListener.class);

    install(new FactoryModuleBuilder().build(GitHubOrganisationGroup.Factory.class));
    install(new FactoryModuleBuilder().build(GitHubGroupMembership.Factory.class));

    install(
        new RestApiModule() {
          @Override
          protected void configure() {
            get(ProjectResource.PROJECT_KIND, "replication").to(ListProjectReplicationStatus.class);
          }
        });

    bind(ReplicationStatusStore.class).to(ReplicationStatusFlatFile.class).in(Scopes.SINGLETON);
    bind(Gson.class).toProvider(GerritGsonProvider.class);
  }
}
