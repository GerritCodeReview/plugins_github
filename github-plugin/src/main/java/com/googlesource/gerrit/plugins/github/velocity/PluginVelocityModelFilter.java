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
package com.googlesource.gerrit.plugins.github.velocity;

import java.io.IOException;
import java.util.Map.Entry;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.google.gerrit.server.IdentifiedUser;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.github.oauth.GitHubLogin;
import com.googlesource.gerrit.plugins.github.oauth.OAuthProtocol.Scope;

@Singleton
public class PluginVelocityModelFilter implements Filter {

  private final Provider<PluginVelocityModel> modelProvider;
  private final Provider<GitHubLogin> loginProvider;
  private final Provider<IdentifiedUser> userProvider;


  @Inject
  public PluginVelocityModelFilter(Provider<PluginVelocityModel> modelProvider,
      Provider<GitHubLogin> loginProvider, Provider<IdentifiedUser> userProvider) {
    this.modelProvider = modelProvider;
    this.loginProvider = loginProvider;
    this.userProvider = userProvider;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response,
      FilterChain chain) throws IOException, ServletException {

    PluginVelocityModel model = modelProvider.get();
    model.put("myself", loginProvider.get().getMyself());
    model.put("user", userProvider.get());
    model.put("hub", loginProvider.get().hub);

    for (Entry<String, String[]> reqPar : request.getParameterMap().entrySet()) {
      model.put(reqPar.getKey(), reqPar.getValue());
    }

    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {
  }
}
