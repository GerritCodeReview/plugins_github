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
package com.googlesource.gerrit.plugins.github.oauth;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jgit.lib.Config;

import com.google.common.base.Objects;
import com.google.gerrit.reviewdb.client.AuthType;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class OAuthConfig {
  private static final String LOGIN_OAUTH_AUTHORIZE = "/login/oauth/authorize";
  private static final String GITHUB_URL = "https://github.com";
  public static final String OAUTH_FINAL = "/oauth";
  public static final String LOGIN_OAUTH_ACCESS_TOKEN =
      "/login/oauth/access_token";
  public static final String OAUTH_LOGIN = "/login";

  public final String gitHubUrl;
  public final String gitHubClientId;
  public final String gitHubClientSecret;
  public final String httpHeader;
  public final String httpDisplaynameHeader;
  public final String httpEmailHeader;
  public final String gitHubOAuthUrl;
  public final String oAuthFinalRedirectUrl;
  public final String gitHubOAuthAccessTokenUrl;
  public final boolean enabled;

  @Inject
  public OAuthConfig(@GerritServerConfig Config config)
      throws MalformedURLException {
    httpHeader = config.getString("auth", null, "httpHeader");
    httpDisplaynameHeader = config.getString("auth", null, "httpDisplaynameHeader");
    httpEmailHeader = config.getString("auth", null, "httpEmailHeader");
    gitHubUrl =
        Objects.firstNonNull(config.getString("github", null, "url"),
            GITHUB_URL);
    gitHubClientId = config.getString("github", null, "clientId");
    gitHubClientSecret = config.getString("github", null, "clientSecret");
    gitHubOAuthUrl = getUrl(gitHubUrl, LOGIN_OAUTH_AUTHORIZE);
    gitHubOAuthAccessTokenUrl = getUrl(gitHubUrl, LOGIN_OAUTH_ACCESS_TOKEN);
    oAuthFinalRedirectUrl =
        getUrl(config.getString("gerrit", null, "canonicalWebUrl"), OAUTH_FINAL);

    enabled =
        config.getString("auth", null, "type").equalsIgnoreCase(
            AuthType.HTTP.toString());
  }

  public String getUrl(String baseUrl, String path)
      throws MalformedURLException {
    if (baseUrl.indexOf("://") > 0) {
      return new URL(new URL(baseUrl), path).toExternalForm();
    } else {
      return baseUrl + (baseUrl.endsWith("/") ? "" : "/")
          + (path.startsWith("/") ? path.substring(1) : path);
    }
  }
}
