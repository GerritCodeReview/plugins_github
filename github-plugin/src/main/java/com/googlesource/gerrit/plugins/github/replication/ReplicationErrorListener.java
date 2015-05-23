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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.common.EventListener;
import com.google.gerrit.extensions.annotations.PluginData;
import com.google.gerrit.reviewdb.client.Project.NameKey;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.RefEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ReplicationErrorListener implements EventListener {
  private static Logger log = LoggerFactory
      .getLogger(ReplicationErrorListener.class);
  private static final String REF_REPLICATED_EVENT = "ref-replicated";
  private static final Gson gson = new GsonBuilder().setPrettyPrinting()
      .create();

  private Path pluginData;

  @Inject
  public ReplicationErrorListener(@PluginData Path pluginData) {
    this.pluginData = pluginData;
  }

  @Override
  public void onEvent(Event event) {
    if (RefEvent.class.isAssignableFrom(event.getClass())
        && event.getType().equals(REF_REPLICATED_EVENT)) {
      RefEvent refEvent = (RefEvent) event;
      NameKey projectNameKey = refEvent.getProjectNameKey();
      JsonObject replicationJson = (JsonObject) gson.toJsonTree(event);

      Path projectPath = pluginData.resolve(projectNameKey.get());
      Path replicationStatusPath = projectPath.resolve("replication-error");

      if (replicationJson.get("status").getAsString().equals("succeeded")) {
        if (Files.exists(replicationStatusPath)) {
          try {
            Files.delete(replicationStatusPath);
          } catch (IOException e) {
            log.error("Unable to delete replication error status "
                + replicationStatusPath, e);
          }
        }
      } else {
        try {
          Files.createDirectories(projectPath);
          Files.write(replicationStatusPath, replicationJson.toString()
              .getBytes(), TRUNCATE_EXISTING, CREATE, WRITE);
        } catch (IOException e) {
          log.error("Unable to write replication status '" + replicationJson
              + "' to " + replicationStatusPath, e);
        }
      }
    }
  }
}
