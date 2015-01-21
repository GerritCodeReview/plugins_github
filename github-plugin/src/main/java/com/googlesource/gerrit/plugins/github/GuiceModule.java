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
import com.google.gerrit.extensions.webui.TopMenu;
import com.google.gerrit.server.account.GroupBackend;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;

import com.googlesource.gerrit.plugins.github.group.GitHubGroupBackend;
import com.googlesource.gerrit.plugins.github.group.GitHubGroupMembership;
import com.googlesource.gerrit.plugins.github.group.GitHubGroupsCache;
import com.googlesource.gerrit.plugins.github.group.GitHubOrganisationGroup;
import com.googlesource.gerrit.plugins.github.oauth.GitHubLogin;
import com.googlesource.gerrit.plugins.github.oauth.IdentifiedUserGitHubLoginProvider;
import com.googlesource.gerrit.plugins.github.oauth.UserScopedProvider;

public class GuiceModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(new TypeLiteral<UserScopedProvider<GitHubLogin>>() {}).to(
        IdentifiedUserGitHubLoginProvider.class);

    install(GitHubGroupsCache.module());

    DynamicSet.bind(binder(), TopMenu.class).to(GitHubTopMenu.class);
    DynamicSet.bind(binder(), GroupBackend.class).to(GitHubGroupBackend.class);

    install(new FactoryModuleBuilder()
        .build(GitHubOrganisationGroup.Factory.class));
    install(new FactoryModuleBuilder()
        .build(GitHubGroupMembership.Factory.class));
  }
}
