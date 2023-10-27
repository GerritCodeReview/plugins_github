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

package com.googlesource.gerrit.plugins.github.oauth;

import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.GERRIT_OAUTH_FINAL;
import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.GITHUB_PLUGIN_OAUTH_SCOPE;

import com.google.common.base.CharMatcher;
import com.google.common.base.MoreObjects;
import com.google.gerrit.httpd.HttpCanonicalWebUrlProvider;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class CannonicalWebUrls {
  private final GitHubOAuthConfig oauthConf;
  private final HttpCanonicalWebUrlProvider canonnicalWebUrlProvider;

  static String trimTrailingSlash(String url) {
    return CharMatcher.is('/').trimTrailingFrom(url);
  }

  @Inject
  CannonicalWebUrls(
      GitHubOAuthConfig oauthConf, HttpCanonicalWebUrlProvider canonicalWebUrlProvider) {
    this.oauthConf = oauthConf;
    this.canonnicalWebUrlProvider = canonicalWebUrlProvider;
  }

  public String getScopeSelectionUrl() {
    return getCannonicalWebUrl()
        + MoreObjects.firstNonNull(oauthConf.scopeSelectionUrl, GITHUB_PLUGIN_OAUTH_SCOPE);
  }

  String getOAuthFinalRedirectUrl() {
    return getCannonicalWebUrl() + GERRIT_OAUTH_FINAL;
  }

  private String getCannonicalWebUrl() {
    return trimTrailingSlash(canonnicalWebUrlProvider.get());
  }
}
