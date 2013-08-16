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
package com.googlesrouce.gerrit.plugins.github.git;

import java.io.File;

import org.eclipse.jgit.lib.Config;

import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.config.SitePaths;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class GitConfig {

  public final File gitDir;

  public GitConfig(File gitDir) {
    this.gitDir = gitDir;
  }

  @Inject
  public GitConfig(final SitePaths site, @GerritServerConfig final Config cfg) {
    gitDir = site.resolve(cfg.getString("gerrit", null, "basePath"));
    if (gitDir == null) {
      throw new IllegalStateException("gerrit.basePath must be configured");
    }
  }
}
