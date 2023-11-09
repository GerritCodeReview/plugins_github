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

package com.googlesource.gerrit.plugins.github;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.googlesource.gerrit.plugins.github.filters.GitHubGroupCacheRefreshFilter;
import com.googlesource.gerrit.plugins.github.group.GitHubGroupsCache;
import com.googlesource.gerrit.plugins.github.groups.OrganizationStructure;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.junit.Before;
import org.junit.Test;

public class GitHubGroupCacheRefreshFilterTest {
  private static final FilterChain NOOP_FILTER_CHAIN_TEST = (req, res) -> {};
  private static final String GITHUB_USERNAME_TEST = "somegithubuser";
  private static final OrganizationStructure GITHUB_USER_ORGANIZATION = new OrganizationStructure();

  private LoadingCache<String, OrganizationStructure> groupsByUsernameCache;
  private GitHubGroupCacheRefreshFilter filter;
  private FakeGroupCacheLoader groupsCacheLoader;
  private int initialLoadCount;

  private static class FakeGroupCacheLoader extends CacheLoader<String, OrganizationStructure> {
    private final String username;
    private final OrganizationStructure organizationStructure;
    private int loadCount;

    FakeGroupCacheLoader(String username, OrganizationStructure organizationStructure) {
      this.username = username;
      this.organizationStructure = organizationStructure;
    }

    @Override
    public OrganizationStructure load(String u) throws Exception {
      if (u.equals(username)) {
        loadCount++;
        return organizationStructure;
      } else {
        return null;
      }
    }

    public int getLoadCount() {
      return loadCount;
    }
  }

  @Before
  public void setUp() throws Exception {
    groupsCacheLoader = new FakeGroupCacheLoader(GITHUB_USERNAME_TEST, GITHUB_USER_ORGANIZATION);
    groupsByUsernameCache = CacheBuilder.newBuilder().build(groupsCacheLoader);
    filter =
        new GitHubGroupCacheRefreshFilter(
            new GitHubGroupsCache(groupsByUsernameCache, () -> GITHUB_USERNAME_TEST));
    // Trigger the initial load of the groups cache
    assertThat(groupsByUsernameCache.get(GITHUB_USERNAME_TEST)).isEqualTo(GITHUB_USER_ORGANIZATION);
    initialLoadCount = groupsCacheLoader.getLoadCount();
  }

  @Test
  public void shouldReloadGroupsUponSuccessfulLogin() throws Exception {
    FakeHttpServletRequest finalLoginRequest = newFinalLoginRequest();
    filter.doFilter(finalLoginRequest, newFinalLoginRedirectWithCookie(), NOOP_FILTER_CHAIN_TEST);
    filter.doFilter(
        newHomepageRequest(finalLoginRequest.getSession()),
        new FakeHttpServletResponse(),
        NOOP_FILTER_CHAIN_TEST);

    assertThat(groupsByUsernameCache.get(GITHUB_USERNAME_TEST)).isEqualTo(GITHUB_USER_ORGANIZATION);
    assertThat(groupsCacheLoader.getLoadCount()).isEqualTo(initialLoadCount + 1);
  }

  @Test
  public void shouldNotReloadGroupsOnRegularRequests() throws Exception {
    FakeHttpServletRequest regularRequest = new FakeHttpServletRequest();
    filter.doFilter(regularRequest, new FakeHttpServletResponse(), NOOP_FILTER_CHAIN_TEST);
    filter.doFilter(
        newHomepageRequest(regularRequest.getSession()),
        new FakeHttpServletResponse(),
        NOOP_FILTER_CHAIN_TEST);

    assertThat(groupsByUsernameCache.get(GITHUB_USERNAME_TEST)).isEqualTo(GITHUB_USER_ORGANIZATION);
    assertThat(groupsCacheLoader.getLoadCount()).isEqualTo(initialLoadCount);
  }

  private ServletRequest newHomepageRequest(HttpSession session) {
    return new FakeHttpServletRequest("/", session);
  }

  private static HttpServletResponse newFinalLoginRedirectWithCookie() {
    HttpServletResponse res = new FakeHttpServletResponse();
    res.setHeader("Set-Cookie", "GerritAccount=foo");
    return res;
  }

  private static FakeHttpServletRequest newFinalLoginRequest() {
    FakeHttpServletRequest req = new FakeHttpServletRequest("/login", null);
    req.setQueryString("final=true");
    return req;
  }
}
