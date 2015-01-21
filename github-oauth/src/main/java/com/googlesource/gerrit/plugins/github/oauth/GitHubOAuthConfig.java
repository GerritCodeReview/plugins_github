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

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.gerrit.reviewdb.client.AuthType;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.googlesource.gerrit.plugins.github.oauth.OAuthProtocol.Scope;

import org.eclipse.jgit.lib.Config;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Singleton
public class GitHubOAuthConfig {
  protected static final String CONF_SECTION = "github";
  private static final String LOGIN_OAUTH_AUTHORIZE = "/login/oauth/authorize";
  private static final String GITHUB_URL = "https://github.com";
  public static final String OAUTH_FINAL = "oauth";
  public static final String LOGIN_OAUTH_ACCESS_TOKEN =
      "/login/oauth/access_token";
  public static final String OAUTH_LOGIN = "/login";
  public static final String OAUTH_LOGOUT = "/logout";

  public final String gitHubUrl;
  public final String gitHubClientId;
  public final String gitHubClientSecret;
  public final String logoutRedirectUrl;
  public final String httpHeader;
  public final String gitHubOAuthUrl;
  public final String oAuthFinalRedirectUrl;
  public final String gitHubOAuthAccessTokenUrl;
  public final boolean enabled;
  public final Map<String, List<OAuthProtocol.Scope>> scopes;
  public final int fileUpdateMaxRetryCount;
  public final int fileUpdateMaxRetryIntervalMsec;
  public final Config gerritConfig;
  public final String oauthHttpHeader;

  @Inject
  public GitHubOAuthConfig(@GerritServerConfig Config config)
      throws MalformedURLException {
    this.gerritConfig = config;

    httpHeader = config.getString("auth", null, "httpHeader");
    oauthHttpHeader = config.getString("auth", null, "httpExternalIdHeader");
    gitHubUrl = dropTrailingSlash(
        MoreObjects.firstNonNull(config.getString(CONF_SECTION, null, "url"),
            GITHUB_URL));
    gitHubClientId = config.getString(CONF_SECTION, null, "clientId");
    gitHubClientSecret = config.getString(CONF_SECTION, null, "clientSecret");
    gitHubOAuthUrl = getUrl(gitHubUrl, LOGIN_OAUTH_AUTHORIZE);
    gitHubOAuthAccessTokenUrl = getUrl(gitHubUrl, LOGIN_OAUTH_ACCESS_TOKEN);
    logoutRedirectUrl = config.getString(CONF_SECTION, null, "logoutRedirectUrl");
    oAuthFinalRedirectUrl =
        getUrl(config.getString("gerrit", null, "canonicalWebUrl"), OAUTH_FINAL);

    enabled =
        config.getString("auth", null, "type").equalsIgnoreCase(
            AuthType.HTTP.toString());
    scopes = getScopes(config);

    fileUpdateMaxRetryCount = config.getInt(CONF_SECTION, "fileUpdateMaxRetryCount", 3);
    fileUpdateMaxRetryIntervalMsec = config.getInt(CONF_SECTION, "fileUpdateMaxRetryIntervalMsec", 3000);
  }

  private Map<String, List<Scope>> getScopes(Config config) {
    Map<String, List<Scope>> scopes = Maps.newHashMap();
    Set<String> configKeys = config.getNames(CONF_SECTION, true);
    for (String key : configKeys) {
      if (key.startsWith("scopes")) {
        String scopesString = config.getString(CONF_SECTION, null, key);
        scopes.put(key, parseScopesString(scopesString));
      }
    }
    return scopes;
  }

  private String dropTrailingSlash(String url) {
    return (url.endsWith("/") ? url.substring(0, url.length()-1):url);
  }

  private List<Scope> parseScopesString(String scopesString) {
    ArrayList<Scope> scopes = new ArrayList<OAuthProtocol.Scope>();
    if(Strings.emptyToNull(scopesString) != null) {
      String[] scopesStrings = scopesString.split(",");
      for (String scope : scopesStrings) {
        scopes.add(Enum.valueOf(Scope.class, scope));
      }
    }

    return scopes;
  }

  private static String getUrl(String baseUrl, String path)
      throws MalformedURLException {
      return new URL(new URL(baseUrl), path).toExternalForm();
  }

  public Scope[] getDefaultScopes() {
    if (scopes == null || scopes.get("scopes") == null) {
      return new Scope[0];
    } else {
      return scopes.get("scopes").toArray(new Scope[0]);
    }
  }
}
