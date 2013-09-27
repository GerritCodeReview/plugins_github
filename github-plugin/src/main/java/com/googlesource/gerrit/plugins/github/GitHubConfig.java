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
package com.googlesource.gerrit.plugins.github;

import java.io.File;
import java.net.MalformedURLException;
import java.util.HashMap;

import org.eclipse.jgit.lib.Config;

import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.config.SitePaths;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig;

@Singleton
public class GitHubConfig extends GitHubOAuthConfig {

  private static final String CONF_WIZARD_FLOW = "wizardFlow";
  private HashMap<String, String> wizardFromTo = new HashMap<String, String>();
  private static final String FROM_TO_SEPARATOR = "=>";

  public final File gitDir;


  @Inject
  public GitHubConfig(@GerritServerConfig Config config, final SitePaths site)
      throws MalformedURLException {
    super(config);
    String[] wizardFlows =
        config.getStringList(CONF_SECTION, null, CONF_WIZARD_FLOW);
    for (String fromTo : wizardFlows) {
      int sepPos = getSepPos(fromTo);
      String fromPage = fromTo.substring(0, sepPos).trim();
      String toPage =
          fromTo.substring(sepPos + FROM_TO_SEPARATOR.length() + 1).trim();
      wizardFromTo.put(fromPage, toPage);
    }
    gitDir = site.resolve(config.getString("gerrit", null, "basePath"));
    if (gitDir == null) {
      throw new IllegalStateException("gerrit.basePath must be configured");
    }
  }

  private int getSepPos(String fromTo) {
    int sepPos = fromTo.indexOf(FROM_TO_SEPARATOR);
    if (sepPos < 0) {
      throw new InvalidGitHubConfigException(fromTo);
    }
    return sepPos;
  }

  public String getNextPage(String sourcePage) {
    return wizardFromTo.get(sourcePage);
  }
}
