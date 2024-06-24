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
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.gerrit.entities.Account;
import com.google.gerrit.server.config.AllProjectsName;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.config.SitePaths;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig;
import java.net.MalformedURLException;
import java.nio.file.Path;
import org.eclipse.jgit.lib.Config;

@Singleton
public class GitHubConfig extends GitHubOAuthConfig {

  private static final String CONF_WIZARD_FLOW = "wizardFlow";
  private static final String FROM_TO_SEPARATOR = "=>";
  private static final String FROM_TO_REDIRECT_SEPARATOR = "R>";
  private static final String CONF_JOB_POOL_LIMIT = "jobPoolLimit";
  private static final String CONF_JOB_EXEC_TIMEOUT = "jobExecTimeout";
  private static final String CONF_PULL_REQUEST_LIST_LIMIT = "pullRequestListLimit";
  private static final String CONF_REPOSITORY_LIST_PAGE_SIZE = "repositoryListPageSize";
  private static final String CONF_REPOSITORY_LIST_LIMIT = "repositoryListLimit";
  private static final String CONF_IGNORE_BRANCH_PROTECTION = "ignoreBranchProtection";
  private static final String CONF_PUBLIC_BASE_PROJECT = "publicBaseProject";
  private static final String CONF_PRIVATE_BASE_PROJECT = "privateBaseProject";
  private static final String CONF_WEBHOOK_SECRET = "webhookSecret";
  private static final String CONF_WEBHOOK_USER = "webhookUser";
  private static final String CONF_IMPORT_ACCOUNT_ID = "importAccountId";
  private static final String DEFAULT_SERVER = "default";

  public final Path gitDir;
  public final int jobPoolLimit;
  public final int jobExecTimeout;
  public final int pullRequestListLimit;
  public final int repositoryListPageSize;
  public final int repositoryListLimit;
  public final boolean ignoreBranchProtection;
  public final String privateBaseProject;
  public final String publicBaseProject;
  public final String allProjectsName;
  public final String webhookSecret;
  public final String webhookUser;
  public final Account.Id importAccountId;
  private final Table<String, String, NextPage> wizardFromTo = HashBasedTable.create();

  public static class NextPage {
    public final String uri;
    public final boolean redirect;

    public NextPage(final String pageUri, final boolean redirect) {
      this.uri = pageUri;
      this.redirect = redirect;
    }
  }

  @Inject
  public GitHubConfig(
      @GerritServerConfig Config config,
      final SitePaths site,
      Provider<AllProjectsName> allProjectsNameProvider)
      throws MalformedURLException {
    super(config);
    parseWizardFlow(config.getStringList(CONF_SECTION, null, CONF_WIZARD_FLOW), DEFAULT_SERVER);

    // Virtual host specific sections
    for (String server : config.getSubsections(CONF_SECTION)) {
      parseWizardFlow(config.getStringList(CONF_SECTION, server, CONF_WIZARD_FLOW), server);
    }

    jobPoolLimit = config.getInt(CONF_SECTION, CONF_JOB_POOL_LIMIT, 5);
    jobExecTimeout = config.getInt(CONF_SECTION, CONF_JOB_EXEC_TIMEOUT, 10);
    pullRequestListLimit = config.getInt(CONF_SECTION, CONF_PULL_REQUEST_LIST_LIMIT, 50);
    repositoryListPageSize = config.getInt(CONF_SECTION, CONF_REPOSITORY_LIST_PAGE_SIZE, 50);
    repositoryListLimit = config.getInt(CONF_SECTION, CONF_REPOSITORY_LIST_LIMIT, 50);
    ignoreBranchProtection = config.getBoolean(CONF_SECTION, CONF_IGNORE_BRANCH_PROTECTION, false);

    gitDir = site.resolve(config.getString("gerrit", null, "basePath"));
    if (gitDir == null) {
      throw new IllegalStateException("gerrit.basePath must be configured");
    }

    privateBaseProject = config.getString(CONF_SECTION, null, CONF_PRIVATE_BASE_PROJECT);
    publicBaseProject = config.getString(CONF_SECTION, null, CONF_PUBLIC_BASE_PROJECT);
    allProjectsName = allProjectsNameProvider.get().toString();
    webhookSecret = config.getString(CONF_SECTION, null, CONF_WEBHOOK_SECRET);
    webhookUser = config.getString(CONF_SECTION, null, CONF_WEBHOOK_USER);
    importAccountId = Account.id(config.getInt(CONF_SECTION, CONF_IMPORT_ACCOUNT_ID, 1000000));
  }

  private void parseWizardFlow(String[] wizardFlows, String server) {
    for (String fromTo : wizardFlows) {
      boolean redirect = fromTo.indexOf(FROM_TO_REDIRECT_SEPARATOR) > 0;
      int sepPos = getSepPos(fromTo, redirect);
      String fromPage = fromTo.substring(0, sepPos).trim();
      NextPage toPage =
          new NextPage(
              fromTo.substring(sepPos + getSeparator(redirect).length() + 1).trim(), redirect);
      wizardFromTo.put(server, fromPage, toPage);
    }
  }

  private static String getSeparator(boolean redirect) {
    String separator = redirect ? FROM_TO_REDIRECT_SEPARATOR : FROM_TO_SEPARATOR;
    return separator;
  }

  private static int getSepPos(String fromTo, boolean redirect) {
    int sepPos = fromTo.indexOf(getSeparator(redirect));
    if (sepPos < 0) {
      throw new InvalidGitHubConfigException(fromTo);
    }
    return sepPos;
  }

  public NextPage getNextPage(String serverName, String sourcePage) {
    if (!wizardFromTo.containsRow(serverName)) {
      return wizardFromTo.get(DEFAULT_SERVER, sourcePage);
    }
    return wizardFromTo.get(serverName, sourcePage);
  }

  public String getBaseProject(boolean isPrivateProject) {
    return MoreObjects.firstNonNull(
        isPrivateProject ? privateBaseProject : publicBaseProject, allProjectsName);
  }
}
