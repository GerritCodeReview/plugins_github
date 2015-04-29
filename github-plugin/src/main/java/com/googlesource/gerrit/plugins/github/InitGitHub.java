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

import java.net.URISyntaxException;

import com.google.common.base.Strings;
import com.google.gerrit.pgm.init.api.ConsoleUI;
import com.google.gerrit.pgm.init.api.InitStep;
import com.google.gerrit.pgm.init.api.InitUtil;
import com.google.gerrit.pgm.init.api.Section;
import com.google.inject.Inject;

public class InitGitHub implements InitStep {
  private static final String GITHUB_URL = "https://github.com";
  private static final String GITHUB_API_URL = "https://api.github.com";
  private static final String GITHUB_REGISTER_APPLICATION_PATH = "/settings/applications/new";
  private static final String GERRIT_OAUTH_CALLBACK_PATH = "oauth";
  
  private final ConsoleUI ui;
  private final Section auth;
  private final Section httpd;
  private final Section github;
  private final Section gerrit;
  
  public enum OAuthType {
    /* Legacy Gerrit/HTTP authentication for GitHub through HTTP Header enrichment */
    HTTP,
    
    /* New native Gerrit/OAuth authentication provider */
    OAUTH
  }

  @Inject
  InitGitHub(final ConsoleUI ui, final Section.Factory sections) {
    this.ui = ui;
    this.github = sections.get("github", null);
    this.httpd = sections.get("httpd", null);
    this.auth = sections.get("auth", null);
    this.gerrit = sections.get("gerrit", null);
  }

  @Override
  public void run() throws Exception {
    ui.header("GitHub Integration");

    github.string("GitHub URL", "url", GITHUB_URL);
    github.string("GitHub API URL", "apiUrl", GITHUB_API_URL);
    ui.message("\nNOTE: You might need to configure a proxy using http.proxy"
        + " if you run Gerrit behind a firewall.\n");

    String gerritUrl = getAssumedCanonicalWebUrl();
    ui.header("GitHub OAuth registration and credentials");
    ui.message(
        "Register Gerrit as GitHub application on:\n" +
        "%s%s\n\n",
        github.get("url"), GITHUB_REGISTER_APPLICATION_PATH);
    ui.message("Settings (assumed Gerrit URL: %s)\n", gerritUrl);
    ui.message("* Application name: Gerrit Code Review\n");
    ui.message("* Homepage URL: %s\n", gerritUrl);
    ui.message("* Authorization callback URL: %s%s\n\n", gerritUrl, GERRIT_OAUTH_CALLBACK_PATH);
    ui.message("After registration is complete, enter the generated OAuth credentials:\n");

    github.string("GitHub Client ID", "clientId", null);
    github.passwordForKey("GitHub Client Secret", "clientSecret");
    
    OAuthType authType = auth.select("Gerrit OAuth implementation", "type", OAuthType.HTTP);
    if (authType.equals(OAuthType.HTTP)) {
      auth.string("HTTP Authentication Header", "httpHeader", "GITHUB_USER");
      httpd.set("filterClass",
          "com.googlesource.gerrit.plugins.github.oauth.OAuthFilter");
      authSetDefault("httpExternalIdHeader", "GITHUB_OAUTH_TOKEN");
      authSetDefault("loginUrl","/login");
      authSetDefault("loginText", "Sign-in with GitHub");
      authSetDefault("registerPageUrl", "/#/register");
    } else {
      httpd.unset("filterClass");
      httpd.unset("httpHeader");
    }
  }

  private void authSetDefault(String key, String defValue) {
    if (Strings.isNullOrEmpty(auth.get(key))) {
      auth.set(key, defValue);
    }
  }

  private String getAssumedCanonicalWebUrl() {
    String url = gerrit.get("canonicalWebUrl");
    if (url != null) {
      return url;
    }

    String httpListen = httpd.get("listenUrl");
    if (httpListen != null) {
      try {
        return InitUtil.toURI(httpListen).toString();
      } catch (URISyntaxException e) {
      }
    }

    return String.format("http://%s:8080/", InitUtil.hostname());
  }

  @Override
  public void postRun() throws Exception {
  }
}
