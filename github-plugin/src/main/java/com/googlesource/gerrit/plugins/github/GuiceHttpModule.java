package com.googlesource.gerrit.plugins.github;

import org.apache.http.client.HttpClient;
import org.apache.velocity.runtime.RuntimeInstance;

import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import com.google.inject.servlet.ServletModule;
import com.googlesource.gerrit.plugins.github.filters.GitHubOAuthFilter;
import com.googlesource.gerrit.plugins.github.filters.PluginVelocityModelFilter;
import com.googlesource.gerrit.plugins.github.oauth.GitHubHttpProvider;
import com.googlesource.gerrit.plugins.github.pullsync.PullRequestsServlet;
import com.googlesource.gerrit.plugins.github.replication.RemoteSiteUser;
import com.googlesource.gerrit.plugins.github.velocity.PluginVelocityRuntimeProvider;
import com.googlesource.gerrit.plugins.github.velocity.VelocityStaticServlet;
import com.googlesource.gerrit.plugins.github.velocity.VelocityViewServlet;
import com.googlesource.gerrit.plugins.github.wizard.VelocityControllerServlet;

public class HttpModule extends ServletModule {

  @Override
  protected void configureServlets() {
    bind(HttpClient.class).toProvider(GitHubHttpProvider.class);

    install(new FactoryModuleBuilder().build(RemoteSiteUser.Factory.class));

    serve("/").with(PullRequestsServlet.class);
    serve("*.css","*.js","*.png","*.jpg","*.woff","*.gif","*.ttf").with(VelocityStaticServlet.class);
    serve("*.html").with(VelocityViewServlet.class);
    serve("*.gh").with(VelocityControllerServlet.class);

    filter("*").through(GitHubOAuthFilter.class);
    filter("*.html").through(PluginVelocityModelFilter.class);

    bind(RuntimeInstance.class).annotatedWith(Names.named("PluginRuntimeInstance"))
        .toProvider(PluginVelocityRuntimeProvider.class);
  }
}
