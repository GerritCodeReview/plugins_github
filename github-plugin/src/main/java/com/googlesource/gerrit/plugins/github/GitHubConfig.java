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

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import com.google.gerrit.server.config.AllProjectsNameProvider;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.config.SitePaths;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig;

import org.eclipse.jgit.lib.Config;

import java.io.File;
import java.net.MalformedURLException;
import java.util.HashMap;

@Singleton
public class GitHubConfig extends GitHubOAuthConfig {

  private static final String CONF_WIZARD_FLOW = "wizardFlow";
  private HashMap<String, NextPage> wizardFromTo = Maps.newHashMap();
  private static final String FROM_TO_SEPARATOR = "=>";
  private static final String FROM_TO_REDIRECT_SEPARATOR = "R>";
  private static final String CONF_JOB_POOL_LIMIT = "jobPoolLimit";
  private static final String CONF_JOB_EXEC_TIMEOUT = "jobExecTimeout";
  private static final String CONF_PULL_REQUEST_LIST_LIMIT =
      "pullRequestListLimit";
  private static final String CONF_REPOSITORY_LIST_PAGE_SIZE =
      "repositoryListPageSize";
  private static final String CONF_REPOSITORY_LIST_LIMIT =
      "repositoryListLimit";
  private static final String CONF_PUBLIC_BASE_PROJECT = "publicBaseProject";
  private static final String CONF_PRIVATE_BASE_PROJECT = "privateBaseProject";

  public final File gitDir;
  public final int jobPoolLimit;
  public final int jobExecTimeout;
  public final int pullRequestListLimit;
  public final int repositoryListPageSize;
  public final int repositoryListLimit;
  public final String privateBaseProject;
  public final String publicBaseProject;
  public final String allProjectsName;

  public static class NextPage {
    public final String uri;
    public final boolean redirect;

    public NextPage(final String pageUri, final boolean redirect) {
      this.uri = pageUri;
      this.redirect = redirect;
    }
  }


  @Inject
  public GitHubConfig(@GerritServerConfig Config config, final SitePaths site, AllProjectsNameProvider allProjectsNameProvider)
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
    }

    jobPoolLimit = config.getInt(CONF_SECTION, CONF_JOB_POOL_LIMIT, 5);
    jobExecTimeout = config.getInt(CONF_SECTION, CONF_JOB_EXEC_TIMEOUT, 10);
    pullRequestListLimit =
        config.getInt(CONF_SECTION, CONF_PULL_REQUEST_LIST_LIMIT, 50);
    repositoryListPageSize =
        config.getInt(CONF_SECTION, CONF_REPOSITORY_LIST_PAGE_SIZE, 50);
    repositoryListLimit =
        config.getInt(CONF_SECTION, CONF_REPOSITORY_LIST_LIMIT, 50);

    gitDir = site.resolve(config.getString("gerrit", null, "basePath"));
    if (gitDir == null) {
      throw new IllegalStateException("gerrit.basePath must be configured");
    }

    privateBaseProject =
        config.getString(CONF_SECTION, null, CONF_PRIVATE_BASE_PROJECT);
    publicBaseProject =
        config.getString(CONF_SECTION, null, CONF_PUBLIC_BASE_PROJECT);
    allProjectsName = allProjectsNameProvider.get().toString();
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

  public String getBaseProject(boolean isPrivateProject) {
    return MoreObjects.firstNonNull(isPrivateProject ? privateBaseProject
        : publicBaseProject, allProjectsName);
  }
}
