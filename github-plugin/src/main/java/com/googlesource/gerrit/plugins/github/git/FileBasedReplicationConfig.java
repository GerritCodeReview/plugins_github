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
package com.googlesource.gerrit.plugins.github.git;

import com.google.gerrit.server.config.SitePaths;
import com.google.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;

public class FileBasedReplicationConfig implements ReplicationConfig {
  private final FileBasedConfig secureConf;
  private final FileBasedConfig replicationConf;

  @Inject
  public FileBasedReplicationConfig(final SitePaths site) {
    replicationConf =
        new FileBasedConfig(new File(site.etc_dir.toFile(), "replication.config"), FS.DETECTED);
    secureConf = new FileBasedConfig(site.secure_config.toFile(), FS.DETECTED);
  }

  @Override
  public synchronized void addSecureCredentials(String authUsername, String authToken)
      throws IOException, ConfigInvalidException {
    secureConf.load();
    secureConf.setString("remote", authUsername, "username", authUsername);
    secureConf.setString("remote", authUsername, "password", authToken);
    secureConf.save();
  }

  @Override
  public synchronized void addReplicationRemote(String username, String url, String projectName)
      throws IOException, ConfigInvalidException {
    replicationConf.load();
    replicationConf.setString("remote", username, "url", url);
    List<String> projects =
        new ArrayList<>(
            Arrays.asList(replicationConf.getStringList("remote", username, "projects")));
    projects.add(projectName);
    replicationConf.setStringList("remote", username, "projects", projects);
    replicationConf.setString("remote", username, "push", "refs/*:refs/*");
    replicationConf.save();
  }
}
