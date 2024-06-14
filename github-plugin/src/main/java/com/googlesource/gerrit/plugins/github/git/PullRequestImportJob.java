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
package com.googlesource.gerrit.plugins.github.git;

import static com.google.gerrit.entities.RefNames.REFS_HEADS;

import com.google.common.collect.Lists;
import com.google.gerrit.entities.Account;
import com.google.gerrit.entities.Change.Id;
import com.google.gerrit.entities.Project;
import com.google.gerrit.entities.Project.NameKey;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.UnprocessableEntityException;
import com.google.gerrit.server.account.AccountImporter;
import com.google.gerrit.server.account.externalids.ExternalId;
import com.google.gerrit.server.account.externalids.ExternalIds;
import com.google.gerrit.server.config.AuthConfig;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.project.ProjectState;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.googlesource.gerrit.plugins.github.git.GitJobStatus.Code;
import com.googlesource.gerrit.plugins.github.oauth.GitHubLogin;
import com.googlesource.gerrit.plugins.github.oauth.ScopedProvider;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.RefSpec;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestCommitDetail;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PullRequestImportJob implements GitJob, ProgressMonitor {

  public interface Factory {
    PullRequestImportJob create(
        @Assisted("index") int jobIndex,
        @Assisted("organisation") String organisation,
        @Assisted("name") String repository,
        @Assisted int pullRequestId,
        @Assisted PullRequestImportType importType);
  }

  private static final Logger LOG = LoggerFactory.getLogger(PullRequestImportJob.class);

  private static final String TOPIC_FORMAT = "GitHub #%d";

  private final GitHubRepository ghRepository;
  private final GitHubLogin ghLogin;
  private final String organisation;
  private final String repoName;
  private final int prId;
  private final GitRepositoryManager repoMgr;
  private final AuthConfig authConfig;
  private final int jobIndex;
  private final ExternalIds externalIds;
  private PullRequestCreateChange createChange;
  private Optional<Project> project;
  private GitJobStatus status;
  private boolean cancelRequested;
  private AccountImporter accountImporter;

  @Inject
  public PullRequestImportJob(
      GitRepositoryManager repoMgr,
      PullRequestCreateChange createChange,
      ProjectCache projectCache,
      AccountImporter accountImporter,
      GitHubRepository.Factory gitHubRepoFactory,
      ScopedProvider<GitHubLogin> ghLoginProvider,
      ExternalIds externalIds,
      AuthConfig authConfig,
      @Assisted("index") int jobIndex,
      @Assisted("organisation") String organisation,
      @Assisted("name") String repoName,
      @Assisted int pullRequestId) {
    this.authConfig = authConfig;
    this.jobIndex = jobIndex;
    this.repoMgr = repoMgr;
    this.ghLogin = ghLoginProvider.get();
    this.organisation = organisation;
    this.repoName = repoName;
    this.prId = pullRequestId;
    this.createChange = createChange;
    this.project = fetchGerritProject(projectCache, organisation, repoName);
    this.ghRepository = gitHubRepoFactory.create(organisation, repoName);
    this.status = new GitJobStatus(jobIndex);
    this.accountImporter = accountImporter;
    this.externalIds = externalIds;
  }

  private Optional<Project> fetchGerritProject(
      ProjectCache projectCache, String fetchOrganisation, String fetchRepoName) {
    NameKey projectNameKey = Project.NameKey.parse(fetchOrganisation + "/" + fetchRepoName);
    return projectCache.get(projectNameKey).map(ProjectState::getProject);
  }

  @Override
  public void run() {
    try {
      status.update(GitJobStatus.Code.SYNC);
      exitWhenCancelled();
      GHPullRequest pr = fetchGitHubPullRequestInfo();

      exitWhenCancelled();
      try (Repository gitRepo =
          repoMgr.openRepository(Project.nameKey(organisation + "/" + repoName))) {
        exitWhenCancelled();
        fetchGitHubPullRequest(gitRepo, pr);

        exitWhenCancelled();
        List<Id> changeIds = addPullRequestToChange(pr, gitRepo);
        status.update(
            GitJobStatus.Code.COMPLETE, "Imported", "PullRequest imported as Changes " + changeIds);
      }
    } catch (JobCancelledException e) {
      status.update(GitJobStatus.Code.CANCELLED);
    } catch (Throwable e) {
      LOG.error(
          "Pull request "
              + prId
              + " into repository "
              + organisation
              + "/"
              + repoName
              + " was failed",
          e);
      status.update(GitJobStatus.Code.FAILED, "Failed", e.getLocalizedMessage());
    }
  }

  private List<Id> addPullRequestToChange(GHPullRequest pr, Repository gitRepo) throws Exception {
    String destinationBranch = REFS_HEADS + pr.getBase().getRef();
    List<Id> prChanges = Lists.newArrayList();
    ObjectId baseObjectId = ObjectId.fromString(pr.getBase().getSha());
    ObjectId prHeadObjectId = ObjectId.fromString(pr.getHead().getSha());

    try (RevWalk walk = new RevWalk(gitRepo)) {
      walk.markUninteresting(walk.lookupCommit(baseObjectId));
      walk.markStart(walk.lookupCommit(prHeadObjectId));
      walk.sort(RevSort.REVERSE);

      int patchNr = 1;
      for (GHPullRequestCommitDetail ghCommitDetail : pr.listCommits()) {
        status.update(
            Code.SYNC,
            "Patch #" + patchNr,
            "Patch#" + patchNr + ": Inserting PullRequest into Gerrit");
        RevCommit revCommit = walk.parseCommit(ObjectId.fromString(ghCommitDetail.getSha()));

        GHUser prUser = pr.getUser();
        GitUser commitAuthor = ghCommitDetail.getCommit().getAuthor();
        GitHubUser gitHubUser = GitHubUser.from(prUser, commitAuthor);

        Account.Id pullRequestOwner = getOrRegisterAccount(gitHubUser);
        if (project.isPresent()) {
          Id changeId =
              createChange.addCommitToChange(
                  project.get(),
                  gitRepo,
                  destinationBranch,
                  pullRequestOwner,
                  revCommit,
                  getChangeMessage(pr),
                  String.format(TOPIC_FORMAT, Integer.valueOf(pr.getNumber())));
          if (changeId != null) {
            prChanges.add(changeId);
          }
        }
      }

      return prChanges;
    }
  }

  private com.google.gerrit.entities.Account.Id getOrRegisterAccount(GitHubUser author)
      throws BadRequestException,
          ResourceConflictException,
          UnprocessableEntityException,
          IOException,
          ConfigInvalidException {
    return getOrRegisterAccount(author.login, author.name, author.email);
  }

  private com.google.gerrit.entities.Account.Id getOrRegisterAccount(
      String login, String name, String email)
      throws BadRequestException,
          ResourceConflictException,
          UnprocessableEntityException,
          IOException,
          ConfigInvalidException {
    Optional<ExternalId> gerritId = externalIdByScheme(ExternalId.SCHEME_GERRIT, login);
    if (gerritId.isPresent()) {
      return gerritId.get().accountId();
    }
    return accountImporter.importAccount(login, name, email);
  }

  private Optional<ExternalId> externalIdByScheme(String scheme, String id) {
    try {
      return externalIds.get(
          ExternalId.Key.create(scheme, id, authConfig.isUserNameCaseInsensitive()));
    } catch (IOException e) {
      LOG.error("Unable to get external id for " + scheme + ":" + id, e);
      return Optional.empty();
    }
  }

  private String getChangeMessage(GHPullRequest pr) {
    return "GitHub Pull Request: "
        + pr.getHtmlUrl()
        + "\n\n"
        + pr.getTitle()
        + "\n\n"
        + pr.getBody();
  }

  private void exitWhenCancelled() throws JobCancelledException {
    if (cancelRequested) {
      throw new JobCancelledException();
    }
  }

  private void fetchGitHubPullRequest(Repository gitRepo, GHPullRequest pr)
      throws GitAPIException, InvalidRemoteException, TransportException {
    status.update(Code.SYNC, "Fetching", "Fetching PullRequests from GitHub");

    try (Git git = Git.wrap(gitRepo)) {
      FetchCommand fetch = git.fetch();
      fetch.setRemote(ghRepository.getCloneUrl());
      fetch.setRefSpecs(
          new RefSpec(
              "+refs/pull/" + pr.getNumber() + "/head:refs/remotes/origin/pr/" + pr.getNumber()));
      fetch.setProgressMonitor(this);
      fetch.setCredentialsProvider(ghRepository.getCredentialsProvider());
      fetch.call();
    }
  }

  private GHPullRequest fetchGitHubPullRequestInfo() throws IOException {
    status.update(Code.SYNC, "Fetch GitHub", "Getting PullRequest info");
    GHPullRequest pr = getGHRepository().getPullRequest(prId);
    return pr;
  }

  @Override
  public GitJobStatus getStatus() {
    return status;
  }

  @Override
  public int getIndex() {
    return jobIndex;
  }

  @Override
  public String getOrganisation() {
    return organisation;
  }

  public GHRepository getGHRepository() throws IOException {
    if (ghLogin.getMyself().getLogin().equals(organisation)) {
      return ghLogin.getMyself().getRepository(repoName);
    }
    return ghLogin.getHub().getOrganization(organisation).getRepository(repoName);
  }

  @Override
  public void cancel() {
    cancelRequested = true;
  }

  @Override
  public String getRepository() {
    return repoName;
  }

  @Override
  public void beginTask(String taskName, int numSteps) {
    status.update(Code.SYNC, taskName, taskName + " ...");
  }

  @Override
  public void endTask() {}

  @Override
  public boolean isCancelled() {
    return cancelRequested;
  }

  @Override
  public void start(int tot) {}

  @Override
  public void update(int progress) {}

  @Override
  public void showDuration(boolean enabled) {}
}
