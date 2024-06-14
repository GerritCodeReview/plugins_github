// Copyright (C) 2012 The Android Open Source Project
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

import com.google.gerrit.entities.Account;
import com.google.gerrit.entities.BranchNameKey;
import com.google.gerrit.entities.Change;
import com.google.gerrit.entities.PatchSet;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.index.query.QueryParseException;
import com.google.gerrit.index.query.QueryResult;
import com.google.gerrit.server.ChangeUtil;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.IdentifiedUser.GenericFactory;
import com.google.gerrit.server.Sequences;
import com.google.gerrit.server.change.ChangeInserter;
import com.google.gerrit.server.change.PatchSetInserter;
import com.google.gerrit.server.notedb.ChangeNotes;
import com.google.gerrit.server.project.InvalidChangeOperationException;
import com.google.gerrit.server.project.NoSuchChangeException;
import com.google.gerrit.server.query.change.ChangeData;
import com.google.gerrit.server.query.change.ChangeQueryBuilder;
import com.google.gerrit.server.query.change.ChangeQueryProcessor;
import com.google.gerrit.server.query.change.InternalChangeQuery;
import com.google.gerrit.server.update.BatchUpdate;
import com.google.gerrit.server.update.UpdateException;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.FooterKey;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.ChangeIdUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PullRequestCreateChange {
  private static final Logger LOG = LoggerFactory.getLogger(PullRequestCreateChange.class);
  private static final FooterKey CHANGE_ID = new FooterKey("Change-Id");

  private final ChangeInserter.Factory changeInserterFactory;
  private final PatchSetInserter.Factory patchSetInserterFactory;
  private final GenericFactory userFactory;
  private final Provider<InternalChangeQuery> queryProvider;
  private final BatchUpdate.Factory updateFactory;
  private final Provider<ChangeQueryProcessor> qp;
  private final ChangeQueryBuilder changeQuery;
  private final Sequences sequences;

  @Inject
  PullRequestCreateChange(
      ChangeInserter.Factory changeInserterFactory,
      PatchSetInserter.Factory patchSetInserterFactory,
      IdentifiedUser.GenericFactory userFactory,
      Provider<InternalChangeQuery> queryProvider,
      BatchUpdate.Factory batchUpdateFactory,
      Provider<ChangeQueryProcessor> qp,
      ChangeQueryBuilder changeQuery,
      Sequences sequences) {
    this.changeInserterFactory = changeInserterFactory;
    this.patchSetInserterFactory = patchSetInserterFactory;
    this.userFactory = userFactory;
    this.queryProvider = queryProvider;
    this.updateFactory = batchUpdateFactory;
    this.qp = qp;
    this.changeQuery = changeQuery;
    this.sequences = sequences;
  }

  public Change.Id addCommitToChange(
      final Project project,
      final Repository repo,
      final String destinationBranch,
      final Account.Id pullRequestOwner,
      final RevCommit pullRequestCommit,
      final String pullRequestMessage,
      final String topic)
      throws NoSuchChangeException,
          IOException,
          InvalidChangeOperationException,
          UpdateException,
          RestApiException {
    try (BatchUpdate bu =
        updateFactory.create(
            project.getNameKey(), userFactory.create(pullRequestOwner), Instant.now())) {

      return internalAddCommitToChange(
          bu,
          project,
          repo,
          destinationBranch,
          pullRequestOwner,
          pullRequestCommit,
          pullRequestMessage,
          topic);
    }
  }

  public Change.Id internalAddCommitToChange(
      BatchUpdate bu,
      final Project project,
      final Repository repo,
      final String destinationBranch,
      final Account.Id pullRequestOwner,
      final RevCommit pullRequestCommit,
      final String pullRequestMesage,
      final String topic)
      throws InvalidChangeOperationException, IOException, UpdateException, RestApiException {
    if (destinationBranch == null || destinationBranch.length() == 0) {
      throw new InvalidChangeOperationException("Destination branch cannot be null or empty");
    }
    Ref destRef = repo.findRef(destinationBranch);
    if (destRef == null) {
      throw new InvalidChangeOperationException("Branch " + destinationBranch + " does not exist.");
    }

    String pullRequestSha1 = pullRequestCommit.getId().getName();
    List<ChangeData> existingChanges = queryChangesForSha1(pullRequestSha1);
    if (!existingChanges.isEmpty()) {
      LOG.debug(
          "Pull request commit ID "
              + pullRequestSha1
              + " has been already uploaded as Change-Id="
              + existingChanges.get(0).getId());
      return null;
    }

    Change.Key changeKey;
    final List<String> idList = pullRequestCommit.getFooterLines(CHANGE_ID);
    if (!idList.isEmpty()) {
      final String idStr = idList.get(idList.size() - 1).trim();
      changeKey = Change.key(idStr);
    } else {
      final ObjectId computedChangeId =
          ChangeIdUtil.computeChangeId(
              pullRequestCommit.getTree(),
              pullRequestCommit,
              pullRequestCommit.getAuthorIdent(),
              pullRequestCommit.getCommitterIdent(),
              pullRequestMesage);

      changeKey = Change.key("I" + computedChangeId.name());
    }

    String branchName = destRef.getName();
    List<ChangeData> destChanges =
        queryProvider
            .get()
            .byBranchKey(
                BranchNameKey.create(
                    project.getNameKey(),
                    branchName.startsWith(REFS_HEADS)
                        ? branchName.substring(REFS_HEADS.length())
                        : branchName),
                changeKey);

    if (destChanges.size() > 1) {
      throw new InvalidChangeOperationException(
          "Multiple Changes with Change-ID "
              + changeKey
              + " already exist on the target branch: cannot add a new patch-set "
              + destinationBranch);
    }

    if (destChanges.size() == 1) {
      // The change key exists on the destination branch: adding a new
      // patch-set
      ChangeData destChangeData = destChanges.get(0);
      Change destChange = destChangeData.change();
      insertPatchSet(
          bu, repo, destChange, pullRequestCommit, destChangeData.notes(), pullRequestMesage);
      return destChange.getId();
    }

    // Change key not found on destination branch. We can create a new
    // change.
    return createNewChange(
        bu,
        changeKey,
        project.getNameKey(),
        destRef,
        pullRequestOwner,
        pullRequestCommit,
        destinationBranch,
        pullRequestMesage,
        topic);
  }

  private List<ChangeData> queryChangesForSha1(String pullRequestSha1) {
    QueryResult<ChangeData> results;
    try {
      results = qp.get().query(changeQuery.commit(pullRequestSha1));
      return results.entities();
    } catch (QueryParseException e) {
      LOG.error(
          "Invalid SHA1 " + pullRequestSha1 + ": cannot query changes for this pull request", e);
      return Collections.emptyList();
    }
  }

  private void insertPatchSet(
      BatchUpdate bu,
      Repository git,
      Change change,
      RevCommit cherryPickCommit,
      ChangeNotes changeNotes,
      String pullRequestMessage)
      throws IOException, UpdateException, RestApiException {
    try (RevWalk revWalk = new RevWalk(git)) {
      PatchSet.Id psId = ChangeUtil.nextPatchSetId(git, change.currentPatchSetId());

      PatchSetInserter patchSetInserter =
          patchSetInserterFactory.create(changeNotes, psId, cherryPickCommit);
      patchSetInserter.setMessage(pullRequestMessage);
      patchSetInserter.setValidate(false);

      bu.addOp(change.getId(), patchSetInserter);
      bu.execute();
    }
  }

  private Change.Id createNewChange(
      BatchUpdate bu,
      Change.Key changeKey,
      Project.NameKey project,
      Ref destRef,
      Account.Id pullRequestOwner,
      RevCommit pullRequestCommit,
      String refName,
      String pullRequestMessage,
      String topic)
      throws UpdateException, RestApiException, IOException {
    Change change =
        new Change(
            changeKey,
            Change.id(sequences.nextChangeId()),
            pullRequestOwner,
            BranchNameKey.create(project, destRef.getName()),
            Instant.now());
    if (topic != null) {
      change.setTopic(topic);
    }
    ChangeInserter ins = changeInserterFactory.create(change.getId(), pullRequestCommit, refName);

    ins.setMessage(pullRequestMessage);
    bu.insertChange(ins);
    bu.execute();

    return ins.getChange().getId();
  }
}
