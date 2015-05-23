// Copyright (C) 2015 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.github.replication;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gerrit.extensions.annotations.PluginData;
import com.google.gson.JsonObject;
import com.google.inject.Inject;

public class ReplicationStatusFlatFile implements ReplicationStatusStore {
  private final Path pluginData;

  @Inject
  public ReplicationStatusFlatFile(@PluginData Path pluginData) {
    this.pluginData = pluginData;
  }

  @Override
  public void set(String key, JsonObject event) throws IOException {
    Path replicationStatusPath = getReplicationStatusPath(key);
    Files.write(replicationStatusPath, event.toString().getBytes(),
        TRUNCATE_EXISTING, CREATE, WRITE);
  }

  @Override
  public void remove(String key) throws IOException {
      Path replicationStatusPath = getReplicationStatusPath(key);
      if (Files.exists(replicationStatusPath)) {
        Files.delete(replicationStatusPath);
      }
  }

  private Path getReplicationStatusPath(String key) throws IOException {
    String sanitizedKey = key.replace(".", "_").replace(" ","_");
    Path projectPath = pluginData.resolve(sanitizedKey + ".replication-error.json");
    Files.createDirectories(projectPath.getParent());
    return projectPath;
  }
}
