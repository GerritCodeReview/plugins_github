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

import com.google.gerrit.extensions.annotations.Exports;
import com.google.gerrit.extensions.auth.oauth.OAuthServiceProvider;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.extensions.webui.JavaScriptPlugin;
import com.google.gerrit.extensions.webui.WebUiPlugin;
import com.google.gerrit.httpd.AllRequestFilter;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import com.google.inject.servlet.ServletModule;
import com.googlesource.gerrit.plugins.github.filters.GitHubGroupCacheRefreshFilter;
import com.googlesource.gerrit.plugins.github.filters.GitHubOAuthFilter;
import com.googlesource.gerrit.plugins.github.git.CreateProjectStep;
import com.googlesource.gerrit.plugins.github.git.GitCloneStep;
import com.googlesource.gerrit.plugins.github.git.GitHubRepository;
import com.googlesource.gerrit.plugins.github.git.GitImporter;
import com.googlesource.gerrit.plugins.github.git.MagicRefCheckStep;
import com.googlesource.gerrit.plugins.github.git.ProtectedBranchesCheckStep;
import com.googlesource.gerrit.plugins.github.git.PullRequestImportJob;
import com.googlesource.gerrit.plugins.github.git.ReplicateProjectStep;
import com.googlesource.gerrit.plugins.github.notification.WebhookServlet;
import com.googlesource.gerrit.plugins.github.oauth.GitHubLogin;
import com.googlesource.gerrit.plugins.github.oauth.PooledHttpClientProvider;
import com.googlesource.gerrit.plugins.github.oauth.ScopedProvider;
import com.googlesource.gerrit.plugins.github.replication.RemoteSiteUser;
import com.googlesource.gerrit.plugins.github.velocity.PluginVelocityRuntimeProvider;
import com.googlesource.gerrit.plugins.github.velocity.VelocityStaticServlet;
import com.googlesource.gerrit.plugins.github.velocity.VelocityViewServlet;
import com.googlesource.gerrit.plugins.github.wizard.VelocityControllerServlet;
import org.apache.http.client.HttpClient;
import org.apache.velocity.runtime.RuntimeInstance;

public class GuiceHttpModule extends ServletModule {

  @Override
  protected void configureServlets() {
    bind(HttpClient.class).toProvider(PooledHttpClientProvider.class);

    bind(new TypeLiteral<ScopedProvider<GitHubLogin>>() {}).to(GitHubLogin.Provider.class);
    bind(new TypeLiteral<ScopedProvider<GitImporter>>() {}).to(GitImporter.Provider.class);

    install(new FactoryModuleBuilder().build(RemoteSiteUser.Factory.class));

    install(
        new FactoryModuleBuilder()
            .implement(ProtectedBranchesCheckStep.class, ProtectedBranchesCheckStep.class)
            .build(ProtectedBranchesCheckStep.Factory.class));
    install(
        new FactoryModuleBuilder()
            .implement(GitCloneStep.class, GitCloneStep.class)
            .build(GitCloneStep.Factory.class));
    install(
        new FactoryModuleBuilder()
            .implement(CreateProjectStep.class, CreateProjectStep.class)
            .build(CreateProjectStep.Factory.class));
    install(
        new FactoryModuleBuilder()
            .implement(ReplicateProjectStep.class, ReplicateProjectStep.class)
            .build(ReplicateProjectStep.Factory.class));
    install(
        new FactoryModuleBuilder()
            .implement(MagicRefCheckStep.class, MagicRefCheckStep.class)
            .build(MagicRefCheckStep.Factory.class));
    install(
        new FactoryModuleBuilder()
            .implement(PullRequestImportJob.class, PullRequestImportJob.class)
            .build(PullRequestImportJob.Factory.class));
    install(
        new FactoryModuleBuilder()
            .implement(GitHubRepository.class, GitHubRepository.class)
            .build(GitHubRepository.Factory.class));

    bind(RuntimeInstance.class)
        .annotatedWith(Names.named("PluginRuntimeInstance"))
        .toProvider(PluginVelocityRuntimeProvider.class);

    bind(String.class).annotatedWith(GitHubURL.class).toProvider(GitHubURLProvider.class);

    bind(OAuthServiceProvider.class)
        .annotatedWith(Exports.named("github"))
        .to(GitHubOAuthServiceProvider.class);

    DynamicSet.bind(binder(), WebUiPlugin.class)
        .toInstance(new JavaScriptPlugin("github-plugin.js"));

    serve("*.css", "*.js", "*.png", "*.jpg", "*.woff", "*.gif", "*.ttf")
        .with(VelocityStaticServlet.class);
    serve("*.gh").with(VelocityControllerServlet.class);
    serve("/webhook").with(WebhookServlet.class);

    serve("/static/*").with(VelocityViewServlet.class);
    filterRegex("(?!/webhook).*").through(GitHubOAuthFilter.class);

    DynamicSet.bind(binder(), AllRequestFilter.class)
        .to(GitHubGroupCacheRefreshFilter.class)
        .in(Scopes.SINGLETON);
  }
}
