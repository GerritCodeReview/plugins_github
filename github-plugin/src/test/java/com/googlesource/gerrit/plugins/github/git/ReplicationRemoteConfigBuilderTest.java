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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gerrit.extensions.registration.DynamicItem;
import com.googlesource.gerrit.plugins.github.oauth.GitHubLogin;
import com.googlesource.gerrit.plugins.github.oauth.ScopedProvider;
import com.googlesource.gerrit.plugins.replication.api.ReplicationRemotesApi;
import org.eclipse.jgit.lib.Config;
import org.junit.Test;
import org.kohsuke.github.GHMyself;

public class ReplicationRemoteConfigBuilderTest {
  private final String repoName = "test-repo";
  private final String username = "test-user";
  private final String password = "myHighlySecretPassword";
  private final String gitHubUrl = "htpps://github.com";

  @Test
  public void shouldBuildConfig() throws Exception {
    ReplicationRemoteConfigBuilder builder = newReplicationRemoteConfigBuilder();
    Config actual = builder.build(repoName);

    assertThat(actual.getString("remote", username, "username")).isEqualTo(username);
    assertThat(actual.getString("remote", username, "password")).isEqualTo(password);
    assertThat(actual.getString("remote", username, "url")).isEqualTo(gitHubUrl + "/${name}.git");
    assertThat(actual.getStringList("remote", username, "projects"))
        .isEqualTo(new String[] {repoName});
    assertThat(actual.getString("remote", username, "push")).isEqualTo("refs/*:refs/*");
  }

  @Test
  public void shouldAppendProjectToConfig() throws Exception {
    String prevProject = "imported-project";
    Config currentConfig = new Config();
    currentConfig.setString("remote", username, "projects", prevProject);

    ReplicationRemoteConfigBuilder builder = newReplicationRemoteConfigBuilder(currentConfig);
    Config actual = builder.build(repoName);

    assertThat(actual.getString("remote", username, "username")).isEqualTo(username);
    assertThat(actual.getString("remote", username, "password")).isEqualTo(password);
    assertThat(actual.getString("remote", username, "url")).isEqualTo(gitHubUrl + "/${name}.git");
    assertThat(actual.getStringList("remote", username, "projects"))
        .isEqualTo(new String[] {prevProject, repoName});
    assertThat(actual.getString("remote", username, "push")).isEqualTo("refs/*:refs/*");
  }

  @Test
  public void shoudKeepCustomUrlAndPushRefSpecInRemoteConfig() throws Exception {
    Config currentConfig = new Config();
    String customPushRefSpec = "+refs/heads/myheads/*:refs/heads/myheads/*";
    String customUrl = "ssh://github@github.com/myuser/${name}.git";
    currentConfig.setString("remote", username, "push", customPushRefSpec);
    currentConfig.setString("remote", username, "url", customUrl);

    ReplicationRemoteConfigBuilder builder = newReplicationRemoteConfigBuilder(currentConfig);
    Config actual = builder.build(repoName);

    assertThat(actual.getString("remote", username, "push")).isEqualTo(customPushRefSpec);
    assertThat(actual.getString("remote", username, "url")).isEqualTo(customUrl);
  }

  private ReplicationRemoteConfigBuilder newReplicationRemoteConfigBuilder() throws Exception {
    return newReplicationRemoteConfigBuilder(new Config());
  }

  private ReplicationRemoteConfigBuilder newReplicationRemoteConfigBuilder(Config currentConfig)
      throws Exception {
    GitHubLogin gitHubLoginMock = mock(GitHubLogin.class);
    GHMyself ghMyselfMock = mock(GHMyself.class);
    ScopedProvider<GitHubLogin> scopedProviderMock = mock(ScopedProvider.class);
    ReplicationRemotesApi replicationRemotesApi = mock(ReplicationRemotesApi.class);
    DynamicItem<ReplicationRemotesApi> replicationRemotesItem = mock(DynamicItem.class);

    when(ghMyselfMock.getLogin()).thenReturn(username);
    when(gitHubLoginMock.getMyself()).thenReturn(ghMyselfMock);
    when(gitHubLoginMock.getAccessToken()).thenReturn(password);
    when(scopedProviderMock.get()).thenReturn(gitHubLoginMock);
    when(replicationRemotesApi.get(username)).thenReturn(currentConfig);
    when(replicationRemotesItem.get()).thenReturn(replicationRemotesApi);

    return new ReplicationRemoteConfigBuilder(
        replicationRemotesItem, scopedProviderMock, gitHubUrl);
  }
}
