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

import org.apache.http.client.HttpClient;

import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.servlet.ServletModule;
import com.googlesource.gerrit.plugins.github.filters.GitHubOAuthFilter;
import com.googlesource.gerrit.plugins.github.oauth.GitHubHttpProvider;
import com.googlesource.gerrit.plugins.github.pullsync.PullRequestsServlet;
import com.googlesource.gerrit.plugins.github.replication.RemoteSiteUser;
import com.googlesource.gerrit.plugins.github.velocity.VelocityStaticServlet;
import com.googlesource.gerrit.plugins.github.velocity.VelocityViewServlet;
import com.googlesource.gerrit.plugins.github.wizard.VelocityControllerServlet;

public class GuiceHttpModule extends ServletModule {

  @Override
  protected void configureServlets() {
    bind(HttpClient.class).toProvider(GitHubHttpProvider.class);
    install(new FactoryModuleBuilder().build(RemoteSiteUser.Factory.class));

    install(new GuiceModule());
    serve("/").with(PullRequestsServlet.class);
    serve("*.css","*.js","*.png","*.jpg","*.woff","*.gif","*.ttf").with(VelocityStaticServlet.class);
    serve("*.html").with(VelocityViewServlet.class);
    serve("*.gh").with(VelocityControllerServlet.class);

    filter("*").through(GitHubOAuthFilter.class);
  }
}
