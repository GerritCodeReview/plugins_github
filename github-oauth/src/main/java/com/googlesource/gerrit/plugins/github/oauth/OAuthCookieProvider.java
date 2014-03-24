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

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.SortedSet;

import javax.servlet.http.Cookie;

import com.google.gerrit.server.config.ConfigUtil;
import com.googlesource.gerrit.plugins.github.oauth.OAuthProtocol.Scope;

public class OAuthCookieProvider {
  private static final String CACHE_NAME = "web_sessions";

  private TokenCipher cipher;
  private GitHubOAuthConfig config;


  public OAuthCookieProvider(TokenCipher cipher, GitHubOAuthConfig config) {
    this.cipher = cipher;
    this.config = config;
  }

  public OAuthCookie getFromUser(String username, String email, String fullName, SortedSet<Scope> scopes) {
    try {
      return new OAuthCookie(cipher, username, email, fullName, scopes, getGerritSessionMaxAgeMillis());
    } catch (OAuthTokenException e) {
      return null;
    }
  }

  public OAuthCookie getFromCookie(Cookie cookie) throws OAuthTokenException {
      return new OAuthCookie(cipher, cookie);
  }

  private long getGerritSessionMaxAgeMillis() {
    return ConfigUtil.getTimeUnit(config.gerritConfig, "cache", CACHE_NAME,
        "maxAge", TokenCipher.MAX_COOKIE_TIMEOUT_SECS, SECONDS);
  }
}
