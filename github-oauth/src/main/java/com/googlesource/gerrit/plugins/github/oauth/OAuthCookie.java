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

import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.http.Cookie;

import com.googlesource.gerrit.plugins.github.oauth.OAuthProtocol.Scope;

public class OAuthCookie extends Cookie {
  private static final long serialVersionUID = 2771690299147135167L;
  public static final String OAUTH_COOKIE_NAME = "GerritOAuth";
  public static final OAuthCookie ANONYMOUS = new OAuthCookie();

  public final String user;
  public final String email;
  public final String fullName;
  public final SortedSet<Scope> scopes;

  public OAuthCookie(TokenCipher cipher, final String user, final String email,
      final String fullName, final SortedSet<Scope> scopes) throws OAuthTokenException {
    super(OAUTH_COOKIE_NAME, cipher.encode(getClearTextCookie(user, email, fullName, scopes)));
    this.user = user;
    this.email = email;
    this.fullName = fullName;
    this.scopes = scopes;
    setMaxAge((int) (TokenCipher.COOKIE_TIMEOUT/1000L));
    setHttpOnly(true);
    setPath("/");
  }

  private static String getClearTextCookie(final String user,
      final String email, final String fullName, final SortedSet<Scope> scopes) {
    StringBuilder clearTextCookie = new StringBuilder();
    clearTextCookie.append(user);
    clearTextCookie.append("\n");
    clearTextCookie.append(email);
    clearTextCookie.append("\n");
    clearTextCookie.append(fullName);

    for (Scope scope : scopes) {
      clearTextCookie.append("\n");
      clearTextCookie.append(scope);
    }
    return clearTextCookie.toString();
  }

  private OAuthCookie() {
    super(OAUTH_COOKIE_NAME, "");
    this.user = "";
    this.scopes = null;
    this.fullName = "";
    this.email = "";
  }

  public OAuthCookie(TokenCipher cipher, Cookie cookie)
      throws OAuthTokenException {
    super(OAUTH_COOKIE_NAME, cookie.getValue());
    String clearTextValue = cipher.decode(cookie.getValue());
    String[] clearText = clearTextValue.split("\n");
    user = clearText.length > 0 ? clearText[0]:null;
    email = clearText.length > 1 ? clearText[1]:null;
    fullName = clearText.length > 2 ? clearText[2]:null;

    this.scopes = new TreeSet<Scope>();
    for (int i = 3; i < clearText.length; i++) {
      Scope scope = Enum.valueOf(Scope.class, clearText[i]);
      this.scopes.add(scope);
    }
  }
}
