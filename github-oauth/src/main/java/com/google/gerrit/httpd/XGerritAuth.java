// Copyright (C) 2014 The Android Open Source Project
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
package com.google.gerrit.httpd;

import com.google.common.cache.Cache;
import com.google.gerrit.httpd.WebSessionManager.Val;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import javax.servlet.http.Cookie;

@Singleton
public class XGerritAuth {
  public static final String X_GERRIT_AUTH = "X-Gerrit-Auth";
  private WebSessionManager manager;

  @Inject
  public XGerritAuth(WebSessionManagerFactory managerFactory,
      @Named(WebSessionManager.CACHE_NAME) Cache<String, Val> cache) {
    this.manager = managerFactory.create(cache);
  }

  public String getAuthValue(Cookie gerritCookie) {
    Val session =
        manager.get(new WebSessionManager.Key(gerritCookie.getValue()));
    return session.getAuth();
  }
}
