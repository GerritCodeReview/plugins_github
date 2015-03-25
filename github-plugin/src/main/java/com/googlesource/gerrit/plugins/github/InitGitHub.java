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

import com.google.gerrit.pgm.init.InitStep;
import com.google.gerrit.pgm.init.Section;
import com.google.gerrit.pgm.util.ConsoleUI;
import com.google.inject.Inject;

public class InitGitHub implements InitStep {
  private final ConsoleUI ui;
  private final Section auth;
  private final Section httpd;
  private final Section github;
  
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
  }

  @Override
  public void run() throws Exception {
    ui.header("GitHub Integration");

    github.string("GitHub URL", "url", "https://github.com");

    boolean gitHubAuth = ui.yesno(true, "Use GitHub for Gerrit login ?");
    if(gitHubAuth) {
      configureAuth();
    }
  }

  private void configureAuth() {
    github.string("GitHub Client ID", "clientId", null);
    github.passwordForKey("GitHub Client Secret", "clientSecret");
    
    OAuthType authType = auth.select("Gerrit OAuth implementation", "type", OAuthType.HTTP);
    if (authType.equals(OAuthType.HTTP)) {
      auth.string("HTTP Authentication Header", "httpHeader", "GITHUB_USER");
      httpd.set("filterClass",
          "com.googlesource.gerrit.plugins.github.oauth.OAuthFilter");
    } else {
      httpd.unset("filterClass");
      httpd.unset("httpHeader");
    }
  }

  @Override
  public void postRun() throws Exception {
  }
}
