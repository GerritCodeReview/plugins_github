// Copyright (C) 2023 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import com.google.gerrit.server.config.SitePaths;
import com.googlesource.gerrit.plugins.github.git.FanoutReplicationConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FanoutReplicationConfigTest {

  private static final String CUSTOM_KEY = "mykey";
  private static final String CUSTOM_VALUE = "myvalue";
  private static final String REMOTE_ENDPOINT = "my-remote-endpoint";
  private static final String TEST_REMOTE_URL = "http://github.com/myurl";
  private static final String TEST_PROJECT_NAME = "myprojectname";
  private Path tempDir;
  private SitePaths sitePaths;

  @Before
  public void setup() throws Exception {
    tempDir = Files.createTempDirectory(getClass().getSimpleName());
    sitePaths = new SitePaths(tempDir);
    Files.createDirectories(sitePaths.etc_dir);
  }

  @After
  public void teardown() throws Exception {
    FileUtils.deleteDirectory(tempDir.toFile());
  }

  @Test
  public void shoudKeepAdHocSettingsInFanoutReplicationConfig() throws Exception {
    FileBasedConfig currConfig = getReplicationConfig();
    currConfig.setString("remote", null, CUSTOM_KEY, CUSTOM_VALUE);
    currConfig.save();

    InMemorySecureStore secureStore = new InMemorySecureStore();

    String url = "http://github.com/myurl";
    FanoutReplicationConfig fanoutReplicationConfig = new FanoutReplicationConfig(sitePaths, secureStore);
    fanoutReplicationConfig.addReplicationRemote(REMOTE_ENDPOINT, url, "myproject");

    currConfig.load();
    assertThat(currConfig.getString("remote", null, CUSTOM_KEY)).isEqualTo(CUSTOM_VALUE);
  }

  @Test
  public void shoudKeepCustomUrlInFanoutReplicationConfig() throws Exception {
    FileBasedConfig currConfig = getReplicationConfig();
    String customUrl = "http://my-custom-url";
    currConfig.setString("remote", null, "url", customUrl);
    currConfig.save();

    InMemorySecureStore secureStore = new InMemorySecureStore();

    new FanoutReplicationConfig(sitePaths, secureStore)
        .addReplicationRemote(REMOTE_ENDPOINT, TEST_REMOTE_URL, TEST_PROJECT_NAME);

    currConfig.load();
    assertThat(currConfig.getString("remote", null, "url")).isEqualTo(customUrl);
  }

  @Test
  public void shoudKeepCustomPushRefSpecInFanoutReplicationConfig() throws Exception {
    FileBasedConfig currConfig = getReplicationConfig();
    String customPushRefSpec = "+refs/heads/myheads/*:refs/heads/myheads/*";
    currConfig.setString("remote", null, "push", customPushRefSpec);
    currConfig.save();

    InMemorySecureStore secureStore = new InMemorySecureStore();

    new FanoutReplicationConfig(sitePaths, secureStore)
        .addReplicationRemote(REMOTE_ENDPOINT, TEST_REMOTE_URL, TEST_PROJECT_NAME);

    currConfig.load();
    assertThat(currConfig.getString("remote", null, "push")).isEqualTo(customPushRefSpec);
  }

  private FileBasedConfig getReplicationConfig() {
    return new FileBasedConfig(
        sitePaths.etc_dir.resolve("replication").resolve(REMOTE_ENDPOINT + ".config").toFile(),
        FS.DETECTED);
  }
}
