// Copyright (C) 2023 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.github.filters;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.httpd.AllRequestFilter;
import com.googlesource.gerrit.plugins.github.group.GitHubGroupsCache;
import java.io.IOException;
import java.util.Optional;
import javax.inject.Inject;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GitHubGroupCacheRefreshFilter extends AllRequestFilter {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private static final String LOGIN_URL = "/login";
  private static final String LOGIN_QUERY_FINAL = "final=true";
  private static final String ACCOUNT_COOKIE = "GerritAccount";
  private static final String INVALIDATE_CACHED_GROUPS = "RefreshGroups";

  private final GitHubGroupsCache ghGroupsCache;

  @Inject
  @VisibleForTesting
  public GitHubGroupCacheRefreshFilter(GitHubGroupsCache ghGroupsCache) {
    this.ghGroupsCache = ghGroupsCache;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {}

  @Override
  public void doFilter(
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
      throws IOException, ServletException {
    filterChain.doFilter(servletRequest, servletResponse);

    HttpServletRequest req = (HttpServletRequest) servletRequest;
    if (req.getRequestURI().endsWith(LOGIN_URL) && req.getQueryString().equals(LOGIN_QUERY_FINAL)) {
      HttpServletResponse resp = (HttpServletResponse) servletResponse;
      String cookieResponse = resp.getHeader("Set-Cookie");
      if (cookieResponse != null && cookieResponse.contains(ACCOUNT_COOKIE)) {
        req.getSession().setAttribute(INVALIDATE_CACHED_GROUPS, Boolean.TRUE);
      }
    } else if (Optional.ofNullable(req.getSession(false))
        .flatMap(session -> Optional.ofNullable(session.getAttribute(INVALIDATE_CACHED_GROUPS)))
        .filter(refresh -> (Boolean) refresh)
        .isPresent()) {
      ghGroupsCache.invalidateCurrentUserGroups();
      req.getSession().removeAttribute(INVALIDATE_CACHED_GROUPS);
    }
  }

  @Override
  public void destroy() {}
}
