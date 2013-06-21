package com.googlesource.gerrit.plugins.github;

import org.apache.http.client.HttpClient;

import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.servlet.ServletModule;
import com.googlesource.gerrit.plugins.github.oauth.GitHubHttpProvider;
import com.googlesource.gerrit.plugins.github.pullsync.PullRequestsServlet;
import com.googlesource.gerrit.plugins.github.replication.RemoteSiteUser;

public class HttpModule extends ServletModule {

  @Override
  protected void configureServlets() {
    bind(HttpClient.class).toProvider(GitHubHttpProvider.class);
    install(new FactoryModuleBuilder().build(RemoteSiteUser.Factory.class));

    serve("/*").with(PullRequestsServlet.class);
  }
}
