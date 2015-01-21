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

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;

import java.io.PrintWriter;


public class GitJobStatus {

  public enum Code {
    SYNC, COMPLETE, FAILED, CANCELLED;

    public String toString() {
      return name().toLowerCase();
    }
  }

  public final int index;
  private Code status;
  private String shortDescription;
  private String value;

  public GitJobStatus(int index) {
    this.index = index;
    this.status = GitJobStatus.Code.SYNC;
    this.shortDescription = "Init";
    this.value = "Initializing ...";
  }

  public void update(Code code, String shortDescription, String description) {
    this.status = code;
    this.shortDescription = shortDescription;
    this.value = description;
  }

  public Code getStatus() {
    return status;
  }

  public String getShortDescription() {
    return shortDescription;
  }

  public String getValue() {
    return value;
  }

  public void update(Code status) {
    this.status = status;
    this.shortDescription = status.name();
    this.value = status.name();
  }

  public void printJson(PrintWriter out) {
    new Gson().toJson(this, GitJobStatus.class, new JsonWriter(out));
  }
}
