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

import java.io.IOException;
import java.util.List;

import com.google.gerrit.reviewdb.client.Project;
import com.google.gson.JsonObject;

public interface ReplicationStatusStore {

  public void set(Project.NameKey projectKey, String refKey,
      JsonObject statusEvent) throws IOException;

  public List<JsonObject> list(Project.NameKey projectKey) throws IOException;
}