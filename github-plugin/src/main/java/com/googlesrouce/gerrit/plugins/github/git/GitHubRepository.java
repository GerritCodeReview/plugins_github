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
package com.googlesrouce.gerrit.plugins.github.git;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.kohsuke.github.*;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.googlesource.gerrit.plugins.github.GitHubURL;
import com.googlesource.gerrit.plugins.github.oauth.GitHubLogin;
import com.googlesource.gerrit.plugins.github.oauth.ScopedProvider;

public class GitHubRepository extends GHRepository {
  public interface Factory {
    GitHubRepository create(@Assisted("organisation") String organisation,
        @Assisted("repository") String repository);
  }


  private final String organisation;
  private final String repository;
  private final GitHubLogin ghLogin;
  private final String cloneUrl;
  private GHRepository ghRepository;

  public String getCloneUrl() {
    return cloneUrl.replace("://", "://" + ghLogin.getMyself().getLogin() + ":"
        + ghLogin.token.access_token + "@");
  }

  public String getOrganisation() {
    return organisation;
  }

  public String getRepository() {
    return repository;
  }

  @Inject
  public GitHubRepository(ScopedProvider<GitHubLogin> ghLoginProvider,
      @GitHubURL String gitHubUrl,
      @Assisted("organisation") String organisation,
      @Assisted("repository") String repository) throws IOException {
    this.cloneUrl = gitHubUrl + "/" + organisation + "/" + repository + ".git";
    this.organisation = organisation;
    this.repository = repository;
    this.ghLogin = ghLoginProvider.get();
    this.ghRepository =
        ghLogin.hub.getRepository(organisation + "/" + repository);
  }

  public String getDescription() {
    return ghRepository.getDescription();
  }

  public String getHomepage() {
    return ghRepository.getHomepage();
  }

  public String getUrl() {
    return ghRepository.getUrl();
  }

  public String getGitTransportUrl() {
    return ghRepository.getGitTransportUrl();
  }

  public String gitHttpTransportUrl() {
    return ghRepository.gitHttpTransportUrl();
  }

  public String getName() {
    return ghRepository.getName();
  }

  public boolean hasPullAccess() {
    return ghRepository.hasPullAccess();
  }

  public boolean hasPushAccess() {
    return ghRepository.hasPushAccess();
  }

  public boolean hasAdminAccess() {
    return ghRepository.hasAdminAccess();
  }

  public String getLanguage() {
    return ghRepository.getLanguage();
  }

  public GHUser getOwner() throws IOException {
    return ghRepository.getOwner();
  }

  public GHIssue getIssue(int id) throws IOException {
    return ghRepository.getIssue(id);
  }

  public GHIssueBuilder createIssue(String title) {
    return ghRepository.createIssue(title);
  }

  public List<GHIssue> getIssues(GHIssueState state) throws IOException {
    return ghRepository.getIssues(state);
  }

  public PagedIterable<GHIssue> listIssues(GHIssueState state) {
    return ghRepository.listIssues(state);
  }

  public GHReleaseBuilder createRelease(String tag) {
    return ghRepository.createRelease(tag);
  }

  public List<GHRelease> getReleases() throws IOException {
    return ghRepository.getReleases();
  }

  public boolean hasIssues() {
    return ghRepository.hasIssues();
  }

  public boolean hasWiki() {
    return ghRepository.hasWiki();
  }

  public boolean isFork() {
    return ghRepository.isFork();
  }

  public int getForks() {
    return ghRepository.getForks();
  }

  public boolean isPrivate() {
    return ghRepository.isPrivate();
  }

  public boolean hasDownloads() {
    return ghRepository.hasDownloads();
  }

  public int getWatchers() {
    return ghRepository.getWatchers();
  }

  public int getOpenIssueCount() {
    return ghRepository.getOpenIssueCount();
  }

  public Date getPushedAt() {
    return ghRepository.getPushedAt();
  }

  public Date getCreatedAt() {
    return ghRepository.getCreatedAt();
  }

  public String getMasterBranch() {
    return ghRepository.getMasterBranch();
  }

  public int getSize() {
    return ghRepository.getSize();
  }

  public GHPersonSet<GHUser> getCollaborators() throws IOException {
    return ghRepository.getCollaborators();
  }

  public Set<String> getCollaboratorNames() throws IOException {
    return ghRepository.getCollaboratorNames();
  }

  public Set<GHTeam> getTeams() throws IOException {
    return ghRepository.getTeams();
  }

  public void addCollaborators(GHUser... users) throws IOException {
    ghRepository.addCollaborators(users);
  }

  public void addCollaborators(Collection<GHUser> users) throws IOException {
    ghRepository.addCollaborators(users);
  }

  public void removeCollaborators(GHUser... users) throws IOException {
    ghRepository.removeCollaborators(users);
  }

  public void removeCollaborators(Collection<GHUser> users) throws IOException {
    ghRepository.removeCollaborators(users);
  }

  public void setEmailServiceHook(String address) throws IOException {
    ghRepository.setEmailServiceHook(address);
  }

  public void enableIssueTracker(boolean v) throws IOException {
    ghRepository.enableIssueTracker(v);
  }

  public void enableWiki(boolean v) throws IOException {
    ghRepository.enableWiki(v);
  }

  public void enableDownloads(boolean v) throws IOException {
    ghRepository.enableDownloads(v);
  }

  public void renameTo(String name) throws IOException {
    ghRepository.renameTo(name);
  }

  public void setDescription(String value) throws IOException {
    ghRepository.setDescription(value);
  }

  public void setHomepage(String value) throws IOException {
    ghRepository.setHomepage(value);
  }

  public void delete() throws IOException {
    ghRepository.delete();
  }

  public GHRepository fork() throws IOException {
    return ghRepository.fork();
  }

  public GHRepository forkTo(GHOrganization org) throws IOException {
    return ghRepository.forkTo(org);
  }

  public GHPullRequest getPullRequest(int i) throws IOException {
    return ghRepository.getPullRequest(i);
  }

  public List<GHPullRequest> getPullRequests(GHIssueState state)
      throws IOException {
    return ghRepository.getPullRequests(state);
  }

  public PagedIterable<GHPullRequest> listPullRequests(GHIssueState state) {
    return ghRepository.listPullRequests(state);
  }

  public List<GHHook> getHooks() throws IOException {
    return ghRepository.getHooks();
  }

  public GHHook getHook(int id) throws IOException {
    return ghRepository.getHook(id);
  }

  public GHCompare getCompare(String id1, String id2) throws IOException {
    return ghRepository.getCompare(id1, id2);
  }

  public GHCompare getCompare(GHCommit id1, GHCommit id2) throws IOException {
    return ghRepository.getCompare(id1, id2);
  }

  public GHCompare getCompare(GHBranch id1, GHBranch id2) throws IOException {
    return ghRepository.getCompare(id1, id2);
  }

  public GHRef[] getRefs() throws IOException {
    return ghRepository.getRefs();
  }

  public GHRef[] getRefs(String refType) throws IOException {
    return ghRepository.getRefs(refType);
  }

  public GHCommit getCommit(String sha1) throws IOException {
    return ghRepository.getCommit(sha1);
  }

  public PagedIterable<GHCommit> listCommits() {
    return ghRepository.listCommits();
  }

  public PagedIterable<GHCommitComment> listCommitComments() {
    return ghRepository.listCommitComments();
  }

  public PagedIterable<GHCommitStatus> listCommitStatuses(String sha1)
      throws IOException {
    return ghRepository.listCommitStatuses(sha1);
  }

  public GHCommitStatus getLastCommitStatus(String sha1) throws IOException {
    return ghRepository.getLastCommitStatus(sha1);
  }

  public GHCommitStatus createCommitStatus(String sha1, GHCommitState state,
      String targetUrl, String description) throws IOException {
    return ghRepository.createCommitStatus(sha1, state, targetUrl, description);
  }

  public PagedIterable<GHEventInfo> listEvents() throws IOException {
    return ghRepository.listEvents();
  }

  public GHHook createHook(String name, Map<String, String> config,
      Collection<GHEvent> events, boolean active) throws IOException {
    return ghRepository.createHook(name, config, events, active);
  }

  public GHHook createWebHook(URL url, Collection<GHEvent> events)
      throws IOException {
    return ghRepository.createWebHook(url, events);
  }

  public GHHook createWebHook(URL url) throws IOException {
    return ghRepository.createWebHook(url);
  }

  @SuppressWarnings("deprecation")
  public Set<URL> getPostCommitHooks() {
    return ghRepository.getPostCommitHooks();
  }

  public Map<String, GHBranch> getBranches() throws IOException {
    return ghRepository.getBranches();
  }

  @SuppressWarnings("deprecation")
  public Map<Integer, GHMilestone> getMilestones() throws IOException {
    return ghRepository.getMilestones();
  }

  public PagedIterable<GHMilestone> listMilestones(GHIssueState state) {
    return ghRepository.listMilestones(state);
  }

  public GHMilestone getMilestone(int number) throws IOException {
    return ghRepository.getMilestone(number);
  }

  public GHContent getFileContent(String path) throws IOException {
    return ghRepository.getFileContent(path);
  }

  public GHContent getFileContent(String path, String ref) throws IOException {
    return ghRepository.getFileContent(path, ref);
  }

  public List<GHContent> getDirectoryContent(String path) throws IOException {
    return ghRepository.getDirectoryContent(path);
  }

  public List<GHContent> getDirectoryContent(String path, String ref)
      throws IOException {
    return ghRepository.getDirectoryContent(path, ref);
  }

  public GHContent getReadme() throws Exception {
    return ghRepository.getReadme();
  }

  public GHContentUpdateResponse createContent(String content,
      String commitMessage, String path) throws IOException {
    return ghRepository.createContent(content, commitMessage, path);
  }

  public GHContentUpdateResponse createContent(String content,
      String commitMessage, String path, String branch) throws IOException {
    return ghRepository.createContent(content, commitMessage, path, branch);
  }

  public GHMilestone createMilestone(String title, String description)
      throws IOException {
    return ghRepository.createMilestone(title, description);
  }

  public String toString() {
    return ghRepository.toString();
  }

  public int hashCode() {
    return ghRepository.hashCode();
  }

  public boolean equals(Object obj) {
    return ghRepository.equals(obj);
  }
}
