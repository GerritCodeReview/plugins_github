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

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.kohsuke.github.GitHub;

import com.google.inject.Inject;
import com.google.inject.servlet.SessionScoped;
import com.googlesource.gerrit.plugins.github.oauth.OAuthProtocol.AccessToken;

@SessionScoped
public class GitHubLogin {
  public AccessToken token;
  public GitHub hub;
  private String redirectUrl;

  private transient OAuthProtocol oauth;

  @Inject
  public GitHubLogin(OAuthProtocol oauth) {
    this.oauth = oauth;
  }

  public GitHubLogin(GitHub hub, AccessToken token) {
    this.hub = hub;
    this.token = token;
  }

  public boolean isLoggedIn() {
    return token != null && hub != null;
  }

  public boolean login(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    if (oauth.isOAuthFinal(request)) {
      init(oauth.loginPhase2(request, response));
      if(isLoggedIn()) {
        response.sendRedirect(redirectUrl);
        return true;
      } else {
        return false;
      }
    } else {
      redirectUrl = request.getRequestURL().toString();
      oauth.loginPhase1(request, response);
      return false;
    }
  }

  private void init(GitHubLogin initValues) {
    this.hub = initValues.hub;
    this.token = initValues.token;
  }
}
