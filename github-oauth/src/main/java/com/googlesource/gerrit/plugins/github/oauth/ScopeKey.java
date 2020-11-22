// Copyright (C) 2018 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.github.oauth;

public class ScopeKey {
  public final String name;
  public final String description;
  public final int sequence;

  public ScopeKey(String name, String description, int sequence) {
    this.name = name;
    this.description = description;
    this.sequence = sequence;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public int getSequence() {
    return sequence;
  }
}