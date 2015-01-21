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
import com.google.inject.Singleton;

import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Singleton
public class ReplicationConfig {
  private final FileBasedConfig secureConf;
  private final FileBasedConfig replicationConf;

  @Inject
  public ReplicationConfig(final SitePaths site) {
    replicationConf =
        new FileBasedConfig(new File(site.etc_dir, "replication.config"),
            FS.DETECTED);
    secureConf = new FileBasedConfig(site.secure_config, FS.DETECTED);
  }

  public synchronized void addSecureCredentials(String organisation,
      String authUsername, String authToken) throws IOException,
      ConfigInvalidException {
    secureConf.load();
    secureConf.setString("remote", organisation, "username", authUsername);
    secureConf.setString("remote", organisation, "password", authToken);
    secureConf.save();
  }

  public synchronized void addReplicationRemote(String organisation,
      String url, String projectName) throws IOException,
      ConfigInvalidException {
    replicationConf.load();
    replicationConf.setString("remote", organisation, "url", url);
    List<String> projects =
        new ArrayList<String>(Arrays.asList(replicationConf.getStringList(
            "remote", organisation, "projects")));
    projects.add(projectName);
    replicationConf.setStringList("remote", organisation, "projects", projects);
    replicationConf.setString("remote", organisation, "push", "refs/*:refs/*");
    replicationConf.save();
  }

}
