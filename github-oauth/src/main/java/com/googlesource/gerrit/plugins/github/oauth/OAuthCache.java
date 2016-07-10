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
package com.googlesource.gerrit.plugins.github.oauth;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gerrit.server.cache.CacheModule;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.googlesource.gerrit.plugins.github.oauth.OAuthProtocol.AccessToken;

import java.util.concurrent.ExecutionException;

@Singleton
public class OAuthCache {
  private static final String CACHE_NAME = "github_oauth";
  
  public static class Loader extends CacheLoader<AccessToken, String> {
    private GitHubLogin ghLogin;

    @Inject
    public Loader(GitHubLogin ghLogin) {
      this.ghLogin = ghLogin;
    }

    @Override
    public String load(AccessToken accessToken) throws Exception {
      ghLogin.login(accessToken);
      return ghLogin.getMyself().getLogin();
    }
  }

  public static Module module() {
    return new CacheModule() {
      @Override
      protected void configure() {
        cache(CACHE_NAME, AccessToken.class, String.class)
          .loader(Loader.class);
        bind(OAuthCache.class);
      }
    };
  }

  private LoadingCache<AccessToken, String> byAccesToken;
  
  @Inject
  public OAuthCache(@Named(CACHE_NAME) LoadingCache<AccessToken, String> byAccessToken) {
    this.byAccesToken = byAccessToken;
  }
  
  public String getLoginByAccessToken(AccessToken accessToken)
      throws ExecutionException {
    return byAccesToken.get(accessToken);
  }
}
