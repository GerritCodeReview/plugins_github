// Copyright (C) 2024 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.github.git;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.kohsuke.github.GHArtifact;
import org.kohsuke.github.GHBlob;
import org.kohsuke.github.GHBlobBuilder;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHCheckRun;
import org.kohsuke.github.GHCheckRunBuilder;
import org.kohsuke.github.GHCodeownersError;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHCommitBuilder;
import org.kohsuke.github.GHCommitComment;
import org.kohsuke.github.GHCommitQueryBuilder;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHCommitStatus;
import org.kohsuke.github.GHCompare;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHContentBuilder;
import org.kohsuke.github.GHContentUpdateResponse;
import org.kohsuke.github.GHDeployKey;
import org.kohsuke.github.GHDeployment;
import org.kohsuke.github.GHDeploymentBuilder;
import org.kohsuke.github.GHDeploymentState;
import org.kohsuke.github.GHDeploymentStatus;
import org.kohsuke.github.GHDeploymentStatusBuilder;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHEventInfo;
import org.kohsuke.github.GHHook;
import org.kohsuke.github.GHInvitation;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueBuilder;
import org.kohsuke.github.GHIssueEvent;
import org.kohsuke.github.GHIssueQueryBuilder;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHLicense;
import org.kohsuke.github.GHMilestone;
import org.kohsuke.github.GHNotificationStream;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHPermissionType;
import org.kohsuke.github.GHPersonSet;
import org.kohsuke.github.GHProject;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestQueryBuilder;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRelease;
import org.kohsuke.github.GHReleaseBuilder;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHRepositoryCloneTraffic;
import org.kohsuke.github.GHRepositoryPublicKey;
import org.kohsuke.github.GHRepositoryStatistics;
import org.kohsuke.github.GHRepositoryVariable;
import org.kohsuke.github.GHRepositoryViewTraffic;
import org.kohsuke.github.GHStargazer;
import org.kohsuke.github.GHSubscription;
import org.kohsuke.github.GHTag;
import org.kohsuke.github.GHTagObject;
import org.kohsuke.github.GHTeam;
import org.kohsuke.github.GHTree;
import org.kohsuke.github.GHTreeBuilder;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GHWorkflow;
import org.kohsuke.github.GHWorkflowJob;
import org.kohsuke.github.GHWorkflowRun;
import org.kohsuke.github.GHWorkflowRunQueryBuilder;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.MarkdownMode;
import org.kohsuke.github.PagedIterable;
import org.kohsuke.github.Preview;
import org.kohsuke.github.function.InputStreamFunction;
import org.kohsuke.github.internal.Previews;

/**
 * This class is generated manually in IntelliJ as replacement for the Lombok @Delegate annotation
 * for generating it automatically.
 *
 * <p>Every time that the GitHub dependency is updated, this class needs to be re-generated from
 * scratch.
 */
class GHRepositoryDelegate extends GHRepository {
  private final GHRepository delegate;

  GHRepositoryDelegate(GHRepository delegate) {
    this.delegate = delegate;
  }

  @Override
  public GHDeploymentBuilder createDeployment(String ref) {
    return delegate.createDeployment(ref);
  }

  @Override
  @Deprecated
  public PagedIterable<GHDeploymentStatus> getDeploymentStatuses(int id) throws IOException {
    return delegate.getDeploymentStatuses(id);
  }

  @Override
  public PagedIterable<GHDeployment> listDeployments(
      String sha, String ref, String task, String environment) {
    return delegate.listDeployments(sha, ref, task, environment);
  }

  @Override
  public GHDeployment getDeployment(long id) throws IOException {
    return delegate.getDeployment(id);
  }

  @Override
  @Deprecated
  public GHDeploymentStatusBuilder createDeployStatus(
      int deploymentId, GHDeploymentState ghDeploymentState) throws IOException {
    return delegate.createDeployStatus(deploymentId, ghDeploymentState);
  }

  @Override
  public String getNodeId() {
    return delegate.getNodeId();
  }

  @Override
  public String getDescription() {
    return delegate.getDescription();
  }

  @Override
  public String getHomepage() {
    return delegate.getHomepage();
  }

  @Override
  public String getGitTransportUrl() {
    return delegate.getGitTransportUrl();
  }

  @Override
  public String getHttpTransportUrl() {
    return delegate.getHttpTransportUrl();
  }

  @Override
  @Deprecated
  public String gitHttpTransportUrl() {
    return delegate.gitHttpTransportUrl();
  }

  @Override
  public String getSvnUrl() {
    return delegate.getSvnUrl();
  }

  @Override
  public String getMirrorUrl() {
    return delegate.getMirrorUrl();
  }

  @Override
  public String getSshUrl() {
    return delegate.getSshUrl();
  }

  @Override
  public URL getHtmlUrl() {
    return delegate.getHtmlUrl();
  }

  @Override
  public String getName() {
    return delegate.getName();
  }

  @Override
  public String getFullName() {
    return delegate.getFullName();
  }

  @Override
  public boolean hasPullAccess() {
    return delegate.hasPullAccess();
  }

  @Override
  public boolean hasPushAccess() {
    return delegate.hasPushAccess();
  }

  @Override
  public boolean hasAdminAccess() {
    return delegate.hasAdminAccess();
  }

  @Override
  public String getLanguage() {
    return delegate.getLanguage();
  }

  @Override
  public GHUser getOwner() throws IOException {
    return delegate.getOwner();
  }

  @Override
  public GHIssue getIssue(int id) throws IOException {
    return delegate.getIssue(id);
  }

  @Override
  public GHIssueBuilder createIssue(String title) {
    return delegate.createIssue(title);
  }

  @Override
  public List<GHIssue> getIssues(GHIssueState state) throws IOException {
    return delegate.getIssues(state);
  }

  @Override
  public List<GHIssue> getIssues(GHIssueState state, GHMilestone milestone) throws IOException {
    return delegate.getIssues(state, milestone);
  }

  @Override
  @Deprecated
  public PagedIterable<GHIssue> listIssues(GHIssueState state) {
    return delegate.listIssues(state);
  }

  @Override
  public GHIssueQueryBuilder.ForRepository queryIssues() {
    return delegate.queryIssues();
  }

  @Override
  public GHReleaseBuilder createRelease(String tag) {
    return delegate.createRelease(tag);
  }

  @Override
  public GHRef createRef(String name, String sha) throws IOException {
    return delegate.createRef(name, sha);
  }

  @Override
  public List<GHRelease> getReleases() throws IOException {
    return delegate.getReleases();
  }

  @Override
  public GHRelease getRelease(long id) throws IOException {
    return delegate.getRelease(id);
  }

  @Override
  public GHRelease getReleaseByTagName(String tag) throws IOException {
    return delegate.getReleaseByTagName(tag);
  }

  @Override
  public GHRelease getLatestRelease() throws IOException {
    return delegate.getLatestRelease();
  }

  @Override
  public PagedIterable<GHRelease> listReleases() throws IOException {
    return delegate.listReleases();
  }

  @Override
  public PagedIterable<GHTag> listTags() throws IOException {
    return delegate.listTags();
  }

  @Override
  public Map<String, Long> listLanguages() throws IOException {
    return delegate.listLanguages();
  }

  @Override
  public String getOwnerName() {
    return delegate.getOwnerName();
  }

  @Override
  public boolean hasIssues() {
    return delegate.hasIssues();
  }

  @Override
  public boolean hasProjects() {
    return delegate.hasProjects();
  }

  @Override
  public boolean hasWiki() {
    return delegate.hasWiki();
  }

  @Override
  public boolean isFork() {
    return delegate.isFork();
  }

  @Override
  public boolean isArchived() {
    return delegate.isArchived();
  }

  @Override
  public boolean isDisabled() {
    return delegate.isDisabled();
  }

  @Override
  public boolean isAllowSquashMerge() {
    return delegate.isAllowSquashMerge();
  }

  @Override
  public boolean isAllowMergeCommit() {
    return delegate.isAllowMergeCommit();
  }

  @Override
  public boolean isAllowRebaseMerge() {
    return delegate.isAllowRebaseMerge();
  }

  @Override
  public boolean isDeleteBranchOnMerge() {
    return delegate.isDeleteBranchOnMerge();
  }

  @Override
  @Deprecated
  public int getForks() {
    return delegate.getForks();
  }

  @Override
  public int getForksCount() {
    return delegate.getForksCount();
  }

  @Override
  public int getStargazersCount() {
    return delegate.getStargazersCount();
  }

  @Override
  public boolean isPrivate() {
    return delegate.isPrivate();
  }

  @Override
  @Preview({Previews.NEBULA})
  public Visibility getVisibility() {
    return delegate.getVisibility();
  }

  @Override
  @Preview({Previews.BAPTISTE})
  public boolean isTemplate() {
    return delegate.isTemplate();
  }

  @Override
  public boolean hasDownloads() {
    return delegate.hasDownloads();
  }

  @Override
  public boolean hasPages() {
    return delegate.hasPages();
  }

  @Override
  @Deprecated
  public int getWatchers() {
    return delegate.getWatchers();
  }

  @Override
  public int getWatchersCount() {
    return delegate.getWatchersCount();
  }

  @Override
  public int getOpenIssueCount() {
    return delegate.getOpenIssueCount();
  }

  @Override
  public int getSubscribersCount() {
    return delegate.getSubscribersCount();
  }

  @Override
  public Date getPushedAt() {
    return delegate.getPushedAt();
  }

  @Override
  public String getDefaultBranch() {
    return delegate.getDefaultBranch();
  }

  @Override
  @Deprecated
  public String getMasterBranch() {
    return delegate.getMasterBranch();
  }

  @Override
  public int getSize() {
    return delegate.getSize();
  }

  @Override
  public GHPersonSet<GHUser> getCollaborators() throws IOException {
    return delegate.getCollaborators();
  }

  @Override
  public PagedIterable<GHUser> listCollaborators() throws IOException {
    return delegate.listCollaborators();
  }

  @Override
  public PagedIterable<GHUser> listCollaborators(CollaboratorAffiliation affiliation)
      throws IOException {
    return delegate.listCollaborators(affiliation);
  }

  @Override
  public PagedIterable<GHUser> listAssignees() throws IOException {
    return delegate.listAssignees();
  }

  @Override
  public boolean hasAssignee(GHUser u) throws IOException {
    return delegate.hasAssignee(u);
  }

  @Override
  public Set<String> getCollaboratorNames() throws IOException {
    return delegate.getCollaboratorNames();
  }

  @Override
  public Set<String> getCollaboratorNames(CollaboratorAffiliation affiliation) throws IOException {
    return delegate.getCollaboratorNames(affiliation);
  }

  @Override
  public boolean isCollaborator(GHUser user) throws IOException {
    return delegate.isCollaborator(user);
  }

  @Override
  public GHPermissionType getPermission(String user) throws IOException {
    return delegate.getPermission(user);
  }

  @Override
  public GHPermissionType getPermission(GHUser u) throws IOException {
    return delegate.getPermission(u);
  }

  @Override
  public boolean hasPermission(String user, GHPermissionType permission) throws IOException {
    return delegate.hasPermission(user, permission);
  }

  @Override
  public boolean hasPermission(GHUser user, GHPermissionType permission) throws IOException {
    return delegate.hasPermission(user, permission);
  }

  @Override
  public Set<GHTeam> getTeams() throws IOException {
    return delegate.getTeams();
  }

  @Override
  @Deprecated
  public void addCollaborators(GHOrganization.Permission permission, GHUser... users)
      throws IOException {
    delegate.addCollaborators(permission, users);
  }

  @Override
  public void addCollaborators(GHOrganization.RepositoryRole permission, GHUser... users)
      throws IOException {
    delegate.addCollaborators(permission, users);
  }

  @Override
  public void addCollaborators(GHUser... users) throws IOException {
    delegate.addCollaborators(users);
  }

  @Override
  public void addCollaborators(Collection<GHUser> users) throws IOException {
    delegate.addCollaborators(users);
  }

  @Override
  @Deprecated
  public void addCollaborators(Collection<GHUser> users, GHOrganization.Permission permission)
      throws IOException {
    delegate.addCollaborators(users, permission);
  }

  @Override
  public void addCollaborators(Collection<GHUser> users, GHOrganization.RepositoryRole permission)
      throws IOException {
    delegate.addCollaborators(users, permission);
  }

  @Override
  public void removeCollaborators(GHUser... users) throws IOException {
    delegate.removeCollaborators(users);
  }

  @Override
  public void removeCollaborators(Collection<GHUser> users) throws IOException {
    delegate.removeCollaborators(users);
  }

  @Override
  public void setEmailServiceHook(String address) throws IOException {
    delegate.setEmailServiceHook(address);
  }

  @Override
  public void enableIssueTracker(boolean v) throws IOException {
    delegate.enableIssueTracker(v);
  }

  @Override
  public void enableProjects(boolean v) throws IOException {
    delegate.enableProjects(v);
  }

  @Override
  public void enableWiki(boolean v) throws IOException {
    delegate.enableWiki(v);
  }

  @Override
  public void enableDownloads(boolean v) throws IOException {
    delegate.enableDownloads(v);
  }

  @Override
  public void renameTo(String name) throws IOException {
    delegate.renameTo(name);
  }

  @Override
  public void setDescription(String value) throws IOException {
    delegate.setDescription(value);
  }

  @Override
  public void setHomepage(String value) throws IOException {
    delegate.setHomepage(value);
  }

  @Override
  public void setDefaultBranch(String value) throws IOException {
    delegate.setDefaultBranch(value);
  }

  @Override
  public void setPrivate(boolean value) throws IOException {
    delegate.setPrivate(value);
  }

  @Override
  @Preview({Previews.NEBULA})
  public void setVisibility(Visibility value) throws IOException {
    delegate.setVisibility(value);
  }

  @Override
  public void allowSquashMerge(boolean value) throws IOException {
    delegate.allowSquashMerge(value);
  }

  @Override
  public void allowMergeCommit(boolean value) throws IOException {
    delegate.allowMergeCommit(value);
  }

  @Override
  public void allowRebaseMerge(boolean value) throws IOException {
    delegate.allowRebaseMerge(value);
  }

  @Override
  public void deleteBranchOnMerge(boolean value) throws IOException {
    delegate.deleteBranchOnMerge(value);
  }

  @Override
  public void delete() throws IOException {
    delegate.delete();
  }

  @Override
  public void archive() throws IOException {
    delegate.archive();
  }

  @Override
  public Updater update() {
    return delegate.update();
  }

  @Override
  public Setter set() {
    return delegate.set();
  }

  @Override
  public PagedIterable<GHRepository> listForks() {
    return delegate.listForks();
  }

  @Override
  public PagedIterable<GHRepository> listForks(ForkSort sort) {
    return delegate.listForks(sort);
  }

  @Override
  public GHRepository fork() throws IOException {
    return delegate.fork();
  }

  @Override
  public GHRepository forkTo(GHOrganization org) throws IOException {
    return delegate.forkTo(org);
  }

  @Override
  public GHPullRequest getPullRequest(int i) throws IOException {
    return delegate.getPullRequest(i);
  }

  @Override
  public List<GHPullRequest> getPullRequests(GHIssueState state) throws IOException {
    return delegate.getPullRequests(state);
  }

  @Override
  @Deprecated
  public PagedIterable<GHPullRequest> listPullRequests(GHIssueState state) {
    return delegate.listPullRequests(state);
  }

  @Override
  public GHPullRequestQueryBuilder queryPullRequests() {
    return delegate.queryPullRequests();
  }

  @Override
  public GHPullRequest createPullRequest(String title, String head, String base, String body)
      throws IOException {
    return delegate.createPullRequest(title, head, base, body);
  }

  @Override
  public GHPullRequest createPullRequest(
      String title, String head, String base, String body, boolean maintainerCanModify)
      throws IOException {
    return delegate.createPullRequest(title, head, base, body, maintainerCanModify);
  }

  @Override
  public GHPullRequest createPullRequest(
      String title,
      String head,
      String base,
      String body,
      boolean maintainerCanModify,
      boolean draft)
      throws IOException {
    return delegate.createPullRequest(title, head, base, body, maintainerCanModify, draft);
  }

  @Override
  public List<GHHook> getHooks() throws IOException {
    return delegate.getHooks();
  }

  @Override
  public GHHook getHook(int id) throws IOException {
    return delegate.getHook(id);
  }

  @Override
  public void deleteHook(int id) throws IOException {
    delegate.deleteHook(id);
  }

  @Override
  public void setCompareUsePaginatedCommits(boolean value) {
    delegate.setCompareUsePaginatedCommits(value);
  }

  @Override
  public GHCompare getCompare(String id1, String id2) throws IOException {
    return delegate.getCompare(id1, id2);
  }

  @Override
  public GHCompare getCompare(GHCommit id1, GHCommit id2) throws IOException {
    return delegate.getCompare(id1, id2);
  }

  @Override
  public GHCompare getCompare(GHBranch id1, GHBranch id2) throws IOException {
    return delegate.getCompare(id1, id2);
  }

  @Override
  public GHRef[] getRefs() throws IOException {
    return delegate.getRefs();
  }

  @Override
  public PagedIterable<GHRef> listRefs() throws IOException {
    return delegate.listRefs();
  }

  @Override
  public GHRef[] getRefs(String refType) throws IOException {
    return delegate.getRefs(refType);
  }

  @Override
  public PagedIterable<GHRef> listRefs(String refType) throws IOException {
    return delegate.listRefs(refType);
  }

  @Override
  public GHRef getRef(String refName) throws IOException {
    return delegate.getRef(refName);
  }

  @Override
  public GHTagObject getTagObject(String sha) throws IOException {
    return delegate.getTagObject(sha);
  }

  @Override
  public GHTree getTree(String sha) throws IOException {
    return delegate.getTree(sha);
  }

  @Override
  public GHTreeBuilder createTree() {
    return delegate.createTree();
  }

  @Override
  public GHTree getTreeRecursive(String sha, int recursive) throws IOException {
    return delegate.getTreeRecursive(sha, recursive);
  }

  @Override
  public GHBlob getBlob(String blobSha) throws IOException {
    return delegate.getBlob(blobSha);
  }

  @Override
  public GHBlobBuilder createBlob() {
    return delegate.createBlob();
  }

  @Override
  public InputStream readBlob(String blobSha) throws IOException {
    return delegate.readBlob(blobSha);
  }

  @Override
  public GHCommit getCommit(String sha1) throws IOException {
    return delegate.getCommit(sha1);
  }

  @Override
  public GHCommitBuilder createCommit() {
    return delegate.createCommit();
  }

  @Override
  public PagedIterable<GHCommit> listCommits() {
    return delegate.listCommits();
  }

  @Override
  public GHCommitQueryBuilder queryCommits() {
    return delegate.queryCommits();
  }

  @Override
  public PagedIterable<GHCommitComment> listCommitComments() {
    return delegate.listCommitComments();
  }

  @Override
  public PagedIterable<GHCommitComment> listCommitComments(String commitSha) {
    return delegate.listCommitComments(commitSha);
  }

  @Override
  public GHLicense getLicense() throws IOException {
    return delegate.getLicense();
  }

  @Override
  public GHContent getLicenseContent() throws IOException {
    return delegate.getLicenseContent();
  }

  @Override
  public PagedIterable<GHCommitStatus> listCommitStatuses(String sha1) throws IOException {
    return delegate.listCommitStatuses(sha1);
  }

  @Override
  public GHCommitStatus getLastCommitStatus(String sha1) throws IOException {
    return delegate.getLastCommitStatus(sha1);
  }

  @Override
  @Preview({Previews.ANTIOPE})
  public PagedIterable<GHCheckRun> getCheckRuns(String ref) throws IOException {
    return delegate.getCheckRuns(ref);
  }

  @Override
  @Preview({Previews.ANTIOPE})
  public PagedIterable<GHCheckRun> getCheckRuns(String ref, Map<String, Object> params)
      throws IOException {
    return delegate.getCheckRuns(ref, params);
  }

  @Override
  public GHCommitStatus createCommitStatus(
      String sha1, GHCommitState state, String targetUrl, String description, String context)
      throws IOException {
    return delegate.createCommitStatus(sha1, state, targetUrl, description, context);
  }

  @Override
  public GHCommitStatus createCommitStatus(
      String sha1, GHCommitState state, String targetUrl, String description) throws IOException {
    return delegate.createCommitStatus(sha1, state, targetUrl, description);
  }

  @Override
  @Preview({Previews.ANTIOPE})
  public GHCheckRunBuilder createCheckRun(String name, String headSHA) {
    return delegate.createCheckRun(name, headSHA);
  }

  @Override
  @Preview({Previews.BAPTISTE})
  public GHCheckRunBuilder updateCheckRun(long checkId) {
    return delegate.updateCheckRun(checkId);
  }

  @Override
  public PagedIterable<GHEventInfo> listEvents() throws IOException {
    return delegate.listEvents();
  }

  @Override
  public PagedIterable<GHLabel> listLabels() throws IOException {
    return delegate.listLabels();
  }

  @Override
  public GHLabel getLabel(String name) throws IOException {
    return delegate.getLabel(name);
  }

  @Override
  public GHLabel createLabel(String name, String color) throws IOException {
    return delegate.createLabel(name, color);
  }

  @Override
  public GHLabel createLabel(String name, String color, String description) throws IOException {
    return delegate.createLabel(name, color, description);
  }

  @Override
  public PagedIterable<GHInvitation> listInvitations() {
    return delegate.listInvitations();
  }

  @Override
  public PagedIterable<GHUser> listSubscribers() {
    return delegate.listSubscribers();
  }

  @Override
  public PagedIterable<GHUser> listStargazers() {
    return delegate.listStargazers();
  }

  @Override
  public PagedIterable<GHStargazer> listStargazers2() {
    return delegate.listStargazers2();
  }

  @Override
  public GHHook createHook(
      String name, Map<String, String> config, Collection<GHEvent> events, boolean active)
      throws IOException {
    return delegate.createHook(name, config, events, active);
  }

  @Override
  public GHHook createWebHook(URL url, Collection<GHEvent> events) throws IOException {
    return delegate.createWebHook(url, events);
  }

  @Override
  public GHHook createWebHook(URL url) throws IOException {
    return delegate.createWebHook(url);
  }

  @Override
  @Deprecated
  public Set<URL> getPostCommitHooks() {
    return delegate.getPostCommitHooks();
  }

  @Override
  public Map<String, GHBranch> getBranches() throws IOException {
    return delegate.getBranches();
  }

  @Override
  public GHBranch getBranch(String name) throws IOException {
    return delegate.getBranch(name);
  }

  @Override
  public Map<Integer, GHMilestone> getMilestones() throws IOException {
    return delegate.getMilestones();
  }

  @Override
  public PagedIterable<GHMilestone> listMilestones(GHIssueState state) {
    return delegate.listMilestones(state);
  }

  @Override
  public GHMilestone getMilestone(int number) throws IOException {
    return delegate.getMilestone(number);
  }

  @Override
  public GHContent getFileContent(String path) throws IOException {
    return delegate.getFileContent(path);
  }

  @Override
  public GHContent getFileContent(String path, String ref) throws IOException {
    return delegate.getFileContent(path, ref);
  }

  @Override
  public List<GHContent> getDirectoryContent(String path) throws IOException {
    return delegate.getDirectoryContent(path);
  }

  @Override
  public List<GHContent> getDirectoryContent(String path, String ref) throws IOException {
    return delegate.getDirectoryContent(path, ref);
  }

  @Override
  public GHContent getReadme() throws IOException {
    return delegate.getReadme();
  }

  @Override
  public void createVariable(String name, String value) throws IOException {
    delegate.createVariable(name, value);
  }

  @Override
  @Deprecated
  public GHRepositoryVariable getRepoVariable(String name) throws IOException {
    return delegate.getRepoVariable(name);
  }

  @Override
  public GHRepositoryVariable getVariable(String name) throws IOException {
    return delegate.getVariable(name);
  }

  @Override
  public GHContentBuilder createContent() {
    return delegate.createContent();
  }

  @Override
  @Deprecated
  public GHContentUpdateResponse createContent(String content, String commitMessage, String path)
      throws IOException {
    return delegate.createContent(content, commitMessage, path);
  }

  @Override
  @Deprecated
  public GHContentUpdateResponse createContent(
      String content, String commitMessage, String path, String branch) throws IOException {
    return delegate.createContent(content, commitMessage, path, branch);
  }

  @Override
  @Deprecated
  public GHContentUpdateResponse createContent(
      byte[] contentBytes, String commitMessage, String path) throws IOException {
    return delegate.createContent(contentBytes, commitMessage, path);
  }

  @Override
  @Deprecated
  public GHContentUpdateResponse createContent(
      byte[] contentBytes, String commitMessage, String path, String branch) throws IOException {
    return delegate.createContent(contentBytes, commitMessage, path, branch);
  }

  @Override
  public GHMilestone createMilestone(String title, String description) throws IOException {
    return delegate.createMilestone(title, description);
  }

  @Override
  public GHDeployKey addDeployKey(String title, String key) throws IOException {
    return delegate.addDeployKey(title, key);
  }

  @Override
  public GHDeployKey addDeployKey(String title, String key, boolean readOnly) throws IOException {
    return delegate.addDeployKey(title, key, readOnly);
  }

  @Override
  public List<GHDeployKey> getDeployKeys() throws IOException {
    return delegate.getDeployKeys();
  }

  @Override
  public GHRepository getSource() throws IOException {
    return delegate.getSource();
  }

  @Override
  public GHRepository getParent() throws IOException {
    return delegate.getParent();
  }

  @Override
  public GHSubscription subscribe(boolean subscribed, boolean ignored) throws IOException {
    return delegate.subscribe(subscribed, ignored);
  }

  @Override
  public GHSubscription getSubscription() throws IOException {
    return delegate.getSubscription();
  }

  @Override
  public List<GHCodeownersError> listCodeownersErrors() throws IOException {
    return delegate.listCodeownersErrors();
  }

  @Override
  public PagedIterable<Contributor> listContributors() throws IOException {
    return delegate.listContributors();
  }

  @Override
  public GHRepositoryStatistics getStatistics() {
    return delegate.getStatistics();
  }

  @Override
  public GHProject createProject(String name, String body) throws IOException {
    return delegate.createProject(name, body);
  }

  @Override
  public PagedIterable<GHProject> listProjects(GHProject.ProjectStateFilter status)
      throws IOException {
    return delegate.listProjects(status);
  }

  @Override
  public PagedIterable<GHProject> listProjects() throws IOException {
    return delegate.listProjects();
  }

  @Override
  public Reader renderMarkdown(String text, MarkdownMode mode) throws IOException {
    return delegate.renderMarkdown(text, mode);
  }

  @Override
  public GHNotificationStream listNotifications() {
    return delegate.listNotifications();
  }

  @Override
  public GHRepositoryViewTraffic getViewTraffic() throws IOException {
    return delegate.getViewTraffic();
  }

  @Override
  public GHRepositoryCloneTraffic getCloneTraffic() throws IOException {
    return delegate.getCloneTraffic();
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return delegate.equals(obj);
  }

  @Override
  public PagedIterable<GHIssueEvent> listIssueEvents() throws IOException {
    return delegate.listIssueEvents();
  }

  @Override
  public GHIssueEvent getIssueEvent(long id) throws IOException {
    return delegate.getIssueEvent(id);
  }

  @Override
  public PagedIterable<GHWorkflow> listWorkflows() {
    return delegate.listWorkflows();
  }

  @Override
  public GHWorkflow getWorkflow(long id) throws IOException {
    return delegate.getWorkflow(id);
  }

  @Override
  public GHWorkflow getWorkflow(String nameOrId) throws IOException {
    return delegate.getWorkflow(nameOrId);
  }

  @Override
  public GHWorkflowRunQueryBuilder queryWorkflowRuns() {
    return delegate.queryWorkflowRuns();
  }

  @Override
  public GHWorkflowRun getWorkflowRun(long id) throws IOException {
    return delegate.getWorkflowRun(id);
  }

  @Override
  public PagedIterable<GHArtifact> listArtifacts() {
    return delegate.listArtifacts();
  }

  @Override
  public GHArtifact getArtifact(long id) throws IOException {
    return delegate.getArtifact(id);
  }

  @Override
  public GHWorkflowJob getWorkflowJob(long id) throws IOException {
    return delegate.getWorkflowJob(id);
  }

  @Override
  public GHRepositoryPublicKey getPublicKey() throws IOException {
    return delegate.getPublicKey();
  }

  @Override
  public List<String> listTopics() throws IOException {
    return delegate.listTopics();
  }

  @Override
  public void setTopics(List<String> topics) throws IOException {
    delegate.setTopics(topics);
  }

  @Override
  public void createSecret(String secretName, String encryptedValue, String publicKeyId)
      throws IOException {
    delegate.createSecret(secretName, encryptedValue, publicKeyId);
  }

  @Override
  public GHTagObject createTag(String tag, String message, String object, String type)
      throws IOException {
    return delegate.createTag(tag, message, object, type);
  }

  @Override
  public <T> T readZip(InputStreamFunction<T> streamFunction, String ref) throws IOException {
    return delegate.readZip(streamFunction, ref);
  }

  @Override
  public <T> T readTar(InputStreamFunction<T> streamFunction, String ref) throws IOException {
    return delegate.readTar(streamFunction, ref);
  }

  @Override
  public <T> void dispatch(String eventType, @Nullable T clientPayload) throws IOException {
    delegate.dispatch(eventType, clientPayload);
  }

  @Override
  public void star() throws IOException {
    delegate.star();
  }

  @Override
  public void unstar() throws IOException {
    delegate.unstar();
  }

  @Override
  @Deprecated
  @CheckForNull
  public Map<String, List<String>> getResponseHeaderFields() {
    return delegate.getResponseHeaderFields();
  }

  @Override
  public Date getCreatedAt() throws IOException {
    return delegate.getCreatedAt();
  }

  @Override
  public URL getUrl() {
    return delegate.getUrl();
  }

  @Override
  public Date getUpdatedAt() throws IOException {
    return delegate.getUpdatedAt();
  }

  @Override
  public long getId() {
    return delegate.getId();
  }

  @Override
  public String toString() {
    return delegate.toString();
  }

  @Override
  @Deprecated
  public GitHub getRoot() {
    return delegate.getRoot();
  }
}
