// Copyright (C) 2024 The Android Open Source Project
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
package com.googlesource.gerrit.plugins.github.git;

import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.github.GitHubURL;
import com.googlesource.gerrit.plugins.github.oauth.GitHubLogin;
import com.googlesource.gerrit.plugins.github.oauth.ScopedProvider;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.googlesource.gerrit.plugins.replication.api.ReplicationConfig;
import org.eclipse.jgit.lib.Config;

class ReplicationRemoteConfigBuilder {
  private final String gitHubUrl;
  private final String username;
  private final String authToken;
  private final DynamicItem<ReplicationConfig> replicationConfigItem;

  @Inject
  ReplicationRemoteConfigBuilder(
      DynamicItem<ReplicationConfig> replicationConfigItem,
      ScopedProvider<GitHubLogin> ghLoginProvider,
      @GitHubURL String gitHubUrl)
      throws IOException {
    this.gitHubUrl = gitHubUrl;
    this.replicationConfigItem = replicationConfigItem;
    GitHubLogin ghLogin = ghLoginProvider.get();
    this.username = ghLogin.getMyself().getLogin();
    this.authToken = ghLogin.getAccessToken();
  }

  Config build(String repositoryName) {
    Config remoteConfig = new Config();

    remoteConfig.setString("remote", username, "username", username);
    remoteConfig.setString("remote", username, "password", authToken);

    remoteConfig.setString("remote", username, "url", gitHubUrl + "/${name}.git");

    String[] existingProjects = getProjects();
    List<String> projects = new ArrayList<>(List.of(existingProjects));
    projects.add(repositoryName);

    remoteConfig.setStringList("remote", username, "projects", projects);
    remoteConfig.setString("remote", username, "push", "refs/*:refs/*");

    return remoteConfig;
  }

  private String[] getProjects() {
    ReplicationConfig config = replicationConfigItem.get();
    if (config != null) {
      return config.getConfig().getStringList("remote", username, "projects");
    }

    return new String[0];
  }
}
