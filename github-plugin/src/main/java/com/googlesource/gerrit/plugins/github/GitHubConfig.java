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

import com.google.common.collect.Maps;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.config.SitePaths;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig;

@Singleton
public class GitHubConfig extends GitHubOAuthConfig {

  private static final String CONF_WIZARD_FLOW = "wizardFlow";
  private HashMap<String, NextPage> wizardFromTo = Maps.newHashMap();
  private static final String FROM_TO_SEPARATOR = "=>";
  private static final String FROM_TO_REDIRECT_SEPARATOR = "R>";
  private static final String CONF_JOB_POOL_LIMIT = "jobPoolLimit";
  private static final String CONF_JOB_EXEC_TIMEOUT = "jobExecTimeout";
  public final File gitDir;
  private int jobPoolLimit;
  private int jobExecTimeout;

  public static class NextPage {
    public final String uri;
    public final boolean redirect;

    public NextPage(final String pageUri, final boolean redirect) {
      this.uri = pageUri;
      this.redirect = redirect;
    }
  }


  @Inject
  public GitHubConfig(@GerritServerConfig Config config, final SitePaths site)
      throws MalformedURLException {
    super(config);
    String[] wizardFlows =
        config.getStringList(CONF_SECTION, null, CONF_WIZARD_FLOW);
    for (String fromTo : wizardFlows) {
      boolean redirect = fromTo.indexOf(FROM_TO_REDIRECT_SEPARATOR) > 0;
      int sepPos = getSepPos(fromTo, redirect);
      String fromPage = fromTo.substring(0, sepPos).trim();
      NextPage toPage =
          new NextPage(fromTo.substring(
              sepPos + getSeparator(redirect).length() + 1).trim(), redirect);
      wizardFromTo.put(fromPage, toPage);

      jobPoolLimit = config.getInt(CONF_SECTION, CONF_JOB_POOL_LIMIT, 5);
      jobExecTimeout = config.getInt(CONF_SECTION, CONF_JOB_EXEC_TIMEOUT, 10);
    }

    gitDir = site.resolve(config.getString("gerrit", null, "basePath"));
    if (gitDir == null) {
      throw new IllegalStateException("gerrit.basePath must be configured");
    }
  }

  private String getSeparator(boolean redirect) {
    String separator =
        redirect ? FROM_TO_REDIRECT_SEPARATOR : FROM_TO_SEPARATOR;
    return separator;
  }

  private int getSepPos(String fromTo, boolean redirect) {
    int sepPos = fromTo.indexOf(getSeparator(redirect));
    if (sepPos < 0) {
      throw new InvalidGitHubConfigException(fromTo);
    }
    return sepPos;
  }

  public NextPage getNextPage(String sourcePage) {
    return wizardFromTo.get(sourcePage);
  }

  public int getJobPoolLimit() {
    return jobPoolLimit;
  }

  public int getJobExecTimeout() {
    return jobExecTimeout;
  }
}
