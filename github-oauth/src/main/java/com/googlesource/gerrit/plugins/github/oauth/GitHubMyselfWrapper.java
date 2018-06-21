// Copyright (C) 2018 The Android Open Source Project
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

import com.infradna.tool.bridge_method_injector.WithBridgeMethods;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.kohsuke.github.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitHubMyselfWrapper extends GHMyself {
  private static final Logger log = LoggerFactory.getLogger(GitHubMyselfWrapper.class);

  private final GHMyself wrapped;

  public GitHubMyselfWrapper(GHMyself wrapped) {
    this.wrapped = wrapped;
  }

  @Override
  public List<String> getEmails() throws IOException {
    return wrapped.getEmails();
  }

  @Override
  public List<GHEmail> getEmails2() throws IOException {
    return wrapped.getEmails2();
  }

  @Override
  public List<GHKey> getPublicKeys() throws IOException {
    return wrapped.getPublicKeys();
  }

  @Override
  public List<GHVerifiedKey> getPublicVerifiedKeys() throws IOException {
    return wrapped.getPublicVerifiedKeys();
  }

  @Override
  public GHPersonSet<GHOrganization> getAllOrganizations() throws IOException {
    try {
      return wrapped.getAllOrganizations();
    } catch (IOException e) {
      log.warn("Unable to list all organizations for user {}", getLogin(), e);
      return new GHPersonSet<>();
    }
  }

  @Override
  public Map<String, GHRepository> getAllRepositories() throws IOException {
    return wrapped.getAllRepositories();
  }

  @Override
  public PagedIterable<GHRepository> listRepositories() {
    return wrapped.listRepositories();
  }

  @Override
  public PagedIterable<GHRepository> listRepositories(int pageSize) {
    return wrapped.listRepositories(pageSize);
  }

  @Override
  public PagedIterable<GHRepository> listRepositories(int pageSize, RepositoryListFilter repoType) {
    return wrapped.listRepositories(pageSize, repoType);
  }

  @Override
  public PagedIterable<GHRepository> listAllRepositories() {
    return wrapped.listAllRepositories();
  }

  @Override
  public void follow() throws IOException {
    wrapped.follow();
  }

  @Override
  public void unfollow() throws IOException {
    wrapped.unfollow();
  }

  @Override
  @WithBridgeMethods({Set.class})
  public GHPersonSet<GHUser> getFollows() throws IOException {
    return wrapped.getFollows();
  }

  @Override
  @WithBridgeMethods({Set.class})
  public GHPersonSet<GHUser> getFollowers() throws IOException {
    return wrapped.getFollowers();
  }

  @Override
  public PagedIterable<GHRepository> listSubscriptions() {
    return wrapped.listSubscriptions();
  }

  @Override
  public boolean isMemberOf(GHOrganization org) {
    return wrapped.isMemberOf(org);
  }

  @Override
  public boolean isMemberOf(GHTeam team) {
    return wrapped.isMemberOf(team);
  }

  @Override
  public boolean isPublicMemberOf(GHOrganization org) {
    return wrapped.isPublicMemberOf(org);
  }

  @Override
  @WithBridgeMethods({Set.class})
  public GHPersonSet<GHOrganization> getOrganizations() throws IOException {
    try {
      return wrapped.getOrganizations();
    } catch (IOException e) {
      log.warn("Unable to list organizations for user {}", getLogin(), e);
      return new GHPersonSet<>();
    }
  }

  @Override
  public PagedIterable<GHEventInfo> listEvents() throws IOException {
    return wrapped.listEvents();
  }

  @Override
  public PagedIterable<GHGist> listGists() throws IOException {
    return wrapped.listGists();
  }

  @Override
  public String toString() {
    return wrapped.toString();
  }

  @Override
  public int hashCode() {
    return wrapped.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return wrapped.equals(obj);
  }

  @Override
  public Map<String, GHRepository> getRepositories() throws IOException {
    return wrapped.getRepositories();
  }

  @Override
  public Iterable<List<GHRepository>> iterateRepositories(int pageSize) {
    return wrapped.iterateRepositories(pageSize);
  }

  @Override
  public GHRepository getRepository(String name) throws IOException {
    return wrapped.getRepository(name);
  }

  @Override
  public String getGravatarId() {
    return wrapped.getGravatarId();
  }

  @Override
  public String getAvatarUrl() {
    return wrapped.getAvatarUrl();
  }

  @Override
  public String getLogin() {
    return wrapped.getLogin();
  }

  @Override
  public String getName() throws IOException {
    return wrapped.getName();
  }

  @Override
  public String getCompany() throws IOException {
    return wrapped.getCompany();
  }

  @Override
  public String getLocation() throws IOException {
    return wrapped.getLocation();
  }

  @Override
  public Date getCreatedAt() throws IOException {
    return wrapped.getCreatedAt();
  }

  @Override
  public Date getUpdatedAt() throws IOException {
    return wrapped.getUpdatedAt();
  }

  @Override
  public String getBlog() throws IOException {
    return wrapped.getBlog();
  }

  @Override
  public URL getHtmlUrl() {
    return wrapped.getHtmlUrl();
  }

  @Override
  public String getEmail() throws IOException {
    return wrapped.getEmail();
  }

  @Override
  public int getPublicGistCount() throws IOException {
    return wrapped.getPublicGistCount();
  }

  @Override
  public int getPublicRepoCount() throws IOException {
    return wrapped.getPublicRepoCount();
  }

  @Override
  public int getFollowingCount() throws IOException {
    return wrapped.getFollowingCount();
  }

  @Override
  public int getFollowersCount() throws IOException {
    return wrapped.getFollowersCount();
  }

  @Override
  @WithBridgeMethods(
      value = {String.class},
      adapterMethod = "urlToString")
  public URL getUrl() {
    return wrapped.getUrl();
  }

  @Override
  @WithBridgeMethods(
      value = {String.class},
      adapterMethod = "intToString")
  public int getId() {
    return wrapped.getId();
  }
}
