// Copyright (C) 2023 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import com.google.gerrit.server.config.AllProjectsName;
import com.google.gerrit.server.config.SitePaths;
import com.google.inject.Provider;
import com.google.inject.util.Providers;
import java.nio.file.Path;
import org.eclipse.jgit.lib.Config;
import org.junit.Before;
import org.junit.Test;

public class GitHubConfigTest {
  private static final Provider<AllProjectsName> ALL_PROJECTS_NAME_PROVIDER =
      Providers.of(new AllProjectsName("All-Projects"));
  private SitePaths site;

  @Before
  public void setup() throws Exception {
    site = new SitePaths(Path.of("/tmp"));
  }

  @Test
  public void getNextPageDefault() throws Exception {
    GitHubConfig.NextPage nextPage =
        newGitHubConfig("wizardFlow = sourcePage => nextPage")
            .getNextPage("anydomain.com", "sourcePage");

    assertThat(nextPage.redirect).isFalse();
    assertThat(nextPage.uri).isEqualTo("nextPage");
  }

  @Test
  public void getNextPageRedirectDefault() throws Exception {
    GitHubConfig.NextPage nextPage =
        newGitHubConfig("wizardFlow = sourcePage R> nextPage")
            .getNextPage("anydomain.com", "sourcePage");

    assertThat(nextPage.redirect).isTrue();
    assertThat(nextPage.uri).isEqualTo("nextPage");
  }

  @Test
  public void getNextPageByDomain() throws Exception {
    GitHubConfig.NextPage nextPage =
        newGitHubConfig(
                "wizardFlow = sourcePage => nextPage\n"
                    + "[github \"anydomain.com\"]\n"
                    + "wizardFlow = sourcePage => customNextPage")
            .getNextPage("anydomain.com", "sourcePage");

    assertThat(nextPage.redirect).isFalse();
    assertThat(nextPage.uri).isEqualTo("customNextPage");
  }

  @Test
  public void getNextPageRedirectByDomain() throws Exception {
    GitHubConfig.NextPage nextPage =
        newGitHubConfig(
                "wizardFlow = sourcePage R> nextPage"
                    + "[github \"anydomain.com\"]\n"
                    + "wizardFlow = sourcePage R> customNextPage")
            .getNextPage("anydomain.com", "sourcePage");

    assertThat(nextPage.redirect).isTrue();
    assertThat(nextPage.uri).isEqualTo("customNextPage");
  }

  private GitHubConfig newGitHubConfig(String configText) throws Exception {
    Config gerritConfig = new Config();
    gerritConfig.fromText(
        "[auth]\n"
            + "httpHeader = GITHUB\n"
            + "type = HTTP\n"
            + "[gerrit]\n"
            + "basePath = /tmp\n"
            + "[github-key \"default\"]\n"
            + "current = true\n"
            + "passwordDevice = /dev/zero\n"
            + "[github]\n"
            + "clientId = myclientid\n"
            + "clientSecret = mysecret\n"
            + configText);
    return new GitHubConfig(gerritConfig, site, ALL_PROJECTS_NAME_PROVIDER, null);
  }
}
