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

import com.google.common.collect.Sets;
import com.google.gerrit.httpd.GitOverHttpServlet;
import com.google.gerrit.httpd.XGerritAuth;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

@Singleton
public class OAuthFilter implements Filter {
  private static final org.slf4j.Logger log = LoggerFactory
      .getLogger(OAuthFilter.class);
  private static Pattern GIT_HTTP_REQUEST_PATTERN = Pattern
      .compile(GitOverHttpServlet.URL_REGEX);
  private static final Set<String> GERRIT_STATIC_RESOURCES_EXTS = Sets
      .newHashSet(Arrays.asList("css", "png", "jpg", "gif", "woff", "otf",
          "ttf", "map", "js", "swf", "txt"));

  private final GitHubOAuthConfig config;
  private final OAuthGitFilter gitFilter;
  private final OAuthWebFilter webFilter;

  @Inject
  public OAuthFilter(GitHubOAuthConfig config,
      OAuthWebFilter webFilter, Injector injector) {
    this.config = config;
    this.webFilter = webFilter;
    Injector childInjector = injector.createChildInjector(OAuthCache.module());
    this.gitFilter = childInjector.getInstance(OAuthGitFilter.class);
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    gitFilter.init(filterConfig);
    webFilter.init(filterConfig);
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response,
      FilterChain chain) throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;

    if (!config.enabled || isStaticResource(httpRequest)
        || isRpcCall(httpRequest) || isAuthenticatedRestCall(httpRequest)) {
      chain.doFilter(request, response);
      return;
    }

    String requestUrl = httpRequest.getRequestURI();
    if (GIT_HTTP_REQUEST_PATTERN.matcher(requestUrl).matches()) {
      gitFilter.doFilter(request, response, chain);
    } else {
      webFilter.doFilter(request, response, chain);
    }
  }

  private boolean isAuthenticatedRestCall(HttpServletRequest httpRequest) {
    return !StringUtils.isEmpty(httpRequest
        .getHeader(XGerritAuth.X_GERRIT_AUTH));
  }

  private boolean isStaticResource(HttpServletRequest httpRequest) {
    String pathExt =
        StringUtils.substringAfterLast(httpRequest.getRequestURI(), ".");
    if (StringUtils.isEmpty(pathExt)) {
      return false;
    }

    return GERRIT_STATIC_RESOURCES_EXTS.contains(pathExt.toLowerCase());
  }

  private boolean isRpcCall(HttpServletRequest httpRequest) {
    return httpRequest.getRequestURI().indexOf("/rpc/") >= 0;
  }

  @Override
  public void destroy() {
    log.info("Destroy");
    gitFilter.destroy();
    webFilter.destroy();
  }
}
