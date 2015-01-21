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

import com.google.common.collect.Lists;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.UnprocessableEntityException;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.reviewdb.client.AccountExternalId;
import com.google.gerrit.reviewdb.client.Change.Id;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.reviewdb.client.Project.NameKey;
import com.google.gerrit.reviewdb.server.AccountExternalIdAccess;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.account.AccountImporter;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.project.ProjectState;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;

import com.googlesource.gerrit.plugins.github.GitHubURL;
import com.googlesource.gerrit.plugins.github.git.GitJobStatus.Code;
import com.googlesource.gerrit.plugins.github.oauth.GitHubLogin;
import com.googlesource.gerrit.plugins.github.oauth.ScopedProvider;

import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
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

import java.io.IOException;
import java.util.List;

public class PullRequestImportJob implements GitJob, ProgressMonitor {

  public interface Factory {
    PullRequestImportJob create(@Assisted("index") int jobIndex,
        @Assisted("organisation") String organisation,
        @Assisted("name") String repository, @Assisted int pullRequestId,
        @Assisted PullRequestImportType importType);
  }

  private static final Logger LOG = LoggerFactory
      .getLogger(PullRequestImportJob.class);

  private static final String TOPIC_FORMAT = "GitHub #%d";

  private final GitHubRepository ghRepository;
  private final GitHubLogin ghLogin;
  private final String organisation;
  private final String repoName;
  private final int prId;
  private final GitRepositoryManager repoMgr;
  private final int jobIndex;
  private PullRequestCreateChange createChange;
  private Project project;
  private GitJobStatus status;
  private boolean cancelRequested;
  private Provider<ReviewDb> schema;
  private AccountImporter accountImporter;

  @Inject
  public PullRequestImportJob(@GitHubURL String gitHubUrl,
      GitRepositoryManager repoMgr, PullRequestCreateChange createChange,
      ProjectCache projectCache,
      Provider<ReviewDb> schema, AccountImporter accountImporter,
      GitHubRepository.Factory gitHubRepoFactory,
      ScopedProvider<GitHubLogin> ghLoginProvider,
      @Assisted("index") int jobIndex,
      @Assisted("organisation") String organisation,
      @Assisted("name") String repoName, @Assisted int pullRequestId) {
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
    this.schema = schema;
    this.accountImporter = accountImporter;
  }

  private Project fetchGerritProject(ProjectCache projectCache,
      String organisation, String repoName) {
    NameKey projectNameKey =
        Project.NameKey.parse(organisation + "/" + repoName);
    ProjectState projectState = projectCache.get(projectNameKey);
    return projectState.getProject();
  }

  @Override
  public void run() {
    ReviewDb db = schema.get();
    try {
      status.update(GitJobStatus.Code.SYNC);
      exitWhenCancelled();
      GHPullRequest pr = fetchGitHubPullRequestInfo();

      exitWhenCancelled();
      Repository gitRepo =
          repoMgr.openRepository(new Project.NameKey(organisation + "/"
              + repoName));
      try {
        exitWhenCancelled();
        fetchGitHubPullRequest(gitRepo, pr);

        exitWhenCancelled();
        List<Id> changeIds = addPullRequestToChange(db, pr, gitRepo);
        status.update(GitJobStatus.Code.COMPLETE, "Imported",
            "PullRequest imported as Changes " + changeIds);
      } finally {
        gitRepo.close();
      }
      db.commit();
    } catch (JobCancelledException e) {
      status.update(GitJobStatus.Code.CANCELLED);
      try {
        db.rollback();
      } catch (OrmException e1) {
        LOG.error("Error rolling back transation", e1);
      }
    } catch (Throwable e) {
      LOG.error("Pull request " + prId + " into repository " + organisation
          + "/" + repoName + " was failed", e);
      status.update(GitJobStatus.Code.FAILED, "Failed",  e.getLocalizedMessage());
      try {
        db.rollback();
      } catch (OrmException e1) {
        LOG.error("Error rolling back transation", e1);
      }
    } finally {
      db.close();
    }
  }

  private List<Id> addPullRequestToChange(ReviewDb db, GHPullRequest pr,
      Repository gitRepo) throws Exception {
    String destinationBranch = pr.getBase().getRef();
    List<Id> prChanges = Lists.newArrayList();
    ObjectId baseObjectId = ObjectId.fromString(pr.getBase().getSha());
    ObjectId prHeadObjectId = ObjectId.fromString(pr.getHead().getSha());

    RevWalk walk = new RevWalk(gitRepo);
    walk.markUninteresting(walk.lookupCommit(baseObjectId));
    walk.markStart(walk.lookupCommit(prHeadObjectId));
    walk.sort(RevSort.REVERSE);

    int patchNr = 1;
    for (GHPullRequestCommitDetail ghCommitDetail : pr.listCommits()) {
      status.update(Code.SYNC, "Patch #" + patchNr, "Patch#" + patchNr
          + ": Inserting PullRequest into Gerrit");
      RevCommit revCommit =
          walk.parseCommit(ObjectId.fromString(ghCommitDetail.getSha()));

      GHUser prUser = pr.getUser();
      GitUser commitAuthor = ghCommitDetail.getCommit().getAuthor();
      GitHubUser gitHubUser = GitHubUser.from(prUser, commitAuthor);

      Account.Id pullRequestOwner = getOrRegisterAccount(db, gitHubUser);
      Id changeId =
          createChange.addCommitToChange(db, project, gitRepo,
              destinationBranch, pullRequestOwner, revCommit,
              getChangeMessage(pr),
              String.format(TOPIC_FORMAT, pr.getNumber()), false);
      if (changeId != null) {
        prChanges.add(changeId);
      }
    }

    return prChanges;
  }

  private com.google.gerrit.reviewdb.client.Account.Id getOrRegisterAccount(
      ReviewDb db, GitHubUser author) throws BadRequestException,
      ResourceConflictException, UnprocessableEntityException, OrmException,
      IOException {
    return getOrRegisterAccount(db, author.getLogin(), author.getName(),
        author.getEmail());
  }

  private com.google.gerrit.reviewdb.client.Account.Id getOrRegisterAccount(
      ReviewDb db, String login, String name, String email)
      throws OrmException, BadRequestException, ResourceConflictException,
      UnprocessableEntityException, IOException {
    AccountExternalId.Key userExtKey =
        new AccountExternalId.Key(AccountExternalId.SCHEME_USERNAME, login);
    AccountExternalIdAccess gerritExtIds = db.accountExternalIds();
    AccountExternalId userExtId = gerritExtIds.get(userExtKey);
    if (userExtId == null) {
      return accountImporter.importAccount(login, name, email);
    } else {
      return userExtId.getAccountId();
    }
  }

  private String getChangeMessage(GHPullRequest pr) {
    return "GitHub Pull Request: " + pr.getUrl() + "\n\n" + pr.getTitle()
        + "\n\n" + pr.getBody().replaceAll("\n", "\n\n");
  }

  private void exitWhenCancelled() throws JobCancelledException {
    if (cancelRequested) {
      throw new JobCancelledException();
    }
  }

  private void fetchGitHubPullRequest(Repository gitRepo, GHPullRequest pr)
      throws GitAPIException, InvalidRemoteException, TransportException {
    status.update(Code.SYNC, "Fetching", "Fetching PullRequests from GitHub");

    Git git = Git.wrap(gitRepo);
    FetchCommand fetch = git.fetch();
    fetch.setRemote(ghRepository.getCloneUrl());
    fetch.setRefSpecs(new RefSpec("+refs/pull/" + pr.getNumber()
        + "/head:refs/remotes/origin/pr/" + pr.getNumber()));
    fetch.setProgressMonitor(this);
    fetch.setCredentialsProvider(ghRepository.getCredentialsProvider());
    fetch.call();
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
    } else {
      return ghLogin.getHub().getOrganization(organisation)
          .getRepository(repoName);
    }
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
  public void endTask() {
  }

  @Override
  public boolean isCancelled() {
    return cancelRequested;
  }

  @Override
  public void start(int tot) {
  }

  @Override
  public void update(int progress) {
  }
}
