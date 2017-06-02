// Copyright (C) 2015 The Android Open Source Project
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

import com.google.common.collect.Sets;
import com.google.gerrit.extensions.auth.oauth.OAuthServiceProvider;
import com.google.gerrit.extensions.auth.oauth.OAuthToken;
import com.google.gerrit.extensions.auth.oauth.OAuthUserInfo;
import com.google.gerrit.extensions.auth.oauth.OAuthVerifier;
import com.google.gerrit.server.account.externalids.ExternalId;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig;
import com.googlesource.gerrit.plugins.github.oauth.OAuthProtocol;
import java.io.IOException;
import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitHubOAuthServiceProvider implements OAuthServiceProvider {
  public static final String VERSION = "2.10.3";
  private static final Logger log = LoggerFactory.getLogger(GitHubOAuthServiceProvider.class);

  private final GitHubOAuthConfig config;
  private final OAuthProtocol oauth;

  @Inject
  public GitHubOAuthServiceProvider(GitHubOAuthConfig config, OAuthProtocol oauth) {
    this.config = config;
    this.oauth = oauth;
  }

  @Override
  public String getAuthorizationUrl() {
    return oauth.getAuthorizationUrl(
        oauth.getScope(Sets.newHashSet(config.getDefaultScopes())), null);
  }

  @Override
  public OAuthToken getAccessToken(OAuthVerifier verifier) {
    try {
      return oauth.getAccessToken(verifier).toOAuthToken();
    } catch (IOException e) {
      log.error("Invalid OAuth access verifier" + verifier.getValue(), e);
      return null;
    }
  }

  @Override
  public OAuthUserInfo getUserInfo(OAuthToken token) throws IOException {
    String oauthToken = token.getToken();
    GitHub hub = GitHub.connectUsingOAuth(oauthToken);
    GHMyself myself = hub.getMyself();
    String login = myself.getLogin();
    return new OAuthUserInfo(
        ExternalId.SCHEME_GERRIT + login, login, myself.getEmail(), myself.getName(), null);
  }

  @Override
  public String getVersion() {
    return VERSION;
  }

  @Override
  public String getName() {
    return "GitHub";
  }
}
