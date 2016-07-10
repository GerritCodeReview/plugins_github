// Copyright (C) 2014 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.github.group;

import com.google.gerrit.common.data.GroupDescription.Basic;
import com.google.gerrit.reviewdb.client.AccountGroup.UUID;

import lombok.Getter;

public abstract class GitHubGroup implements Basic {
  public static final String UUID_PREFIX = "github:";
  public static final String NAME_PREFIX = "github/";

  @Getter
  protected final UUID groupUUID;

  @Getter
  protected final String url;

  GitHubGroup(UUID groupUUID, String url) {
    this.groupUUID = groupUUID;
    this.url = url;
  }

  @Override
  public String getEmailAddress() {
    return "";
  }
}
