package com.googlesource.gerrit.plugins.github.filters;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.github.oauth.GitHubLogin;
import com.googlesource.gerrit.plugins.github.oauth.OAuthProtocol.Scope;

@Singleton
public class GitHubOAuthFilter implements Filter {
  
  private final Provider<GitHubLogin> loginProvider;

  @Inject
  public GitHubOAuthFilter(final Provider<GitHubLogin> loginProvider) {
    this.loginProvider = loginProvider;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response,
      FilterChain chain) throws IOException, ServletException {
    GitHubLogin hubLogin = loginProvider.get();
    if (!hubLogin.isLoggedIn(Scope.USER)) {
      hubLogin.login(request, response, Scope.USER);
      return;
    } else {
      chain.doFilter(request, response);
    }
  }

  @Override
  public void destroy() {
  }

}
