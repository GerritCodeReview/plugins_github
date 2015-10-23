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

import static com.google.gerrit.reviewdb.client.RefNames.REFS_HEADS;

import com.google.gerrit.common.TimeUtil;
import com.google.gerrit.common.errors.EmailException;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.reviewdb.client.Branch;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.reviewdb.client.PatchSet;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.reviewdb.client.RevId;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.ChangeUtil;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.IdentifiedUser.GenericFactory;
import com.google.gerrit.server.change.ChangeInserter;
import com.google.gerrit.server.change.PatchSetInserter;
import com.google.gerrit.server.git.BatchUpdate;
import com.google.gerrit.server.git.MergeException;
import com.google.gerrit.server.git.UpdateException;
import com.google.gerrit.server.git.validators.CommitValidators.Policy;
import com.google.gerrit.server.project.InvalidChangeOperationException;
import com.google.gerrit.server.project.NoSuchChangeException;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.gerrit.server.project.ProjectControl;
import com.google.gerrit.server.project.RefControl;
import com.google.gerrit.server.query.change.ChangeData;
import com.google.gerrit.server.query.change.InternalChangeQuery;
import com.google.gwtorm.server.OrmException;
import com.google.gwtorm.server.ResultSet;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.FooterKey;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.ChangeIdUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class PullRequestCreateChange {
  private static final Logger LOG = LoggerFactory
      .getLogger(PullRequestCreateChange.class);
  private static final FooterKey CHANGE_ID = new FooterKey("Change-Id");

  private final ChangeInserter.Factory changeInserterFactory;
  private final PatchSetInserter.Factory patchSetInserterFactory;
  private final ProjectControl.Factory projectControlFactory;
  private final GenericFactory userFactory;
  private final Provider<InternalChangeQuery> queryProvider;
  private final BatchUpdate.Factory updateFactory;

  @Inject
  PullRequestCreateChange(ChangeInserter.Factory changeInserterFactory,
      PatchSetInserter.Factory patchSetInserterFactory,
      ProjectControl.Factory projectControlFactory,
      IdentifiedUser.GenericFactory userFactory,
      Provider<InternalChangeQuery> queryProvider,
      BatchUpdate.Factory batchUpdateFactory) {
    this.changeInserterFactory = changeInserterFactory;
    this.patchSetInserterFactory = patchSetInserterFactory;
    this.projectControlFactory = projectControlFactory;
    this.userFactory = userFactory;
    this.queryProvider = queryProvider;
    this.updateFactory = batchUpdateFactory;
  }

  public Change.Id addCommitToChange(ReviewDb db, final Project project,
      final Repository repo, final String destinationBranch,
      final Account.Id pullRequestOwner, final RevCommit pullRequestCommit,
      final String pullRequestMessage, final String topic)
      throws NoSuchChangeException, EmailException, OrmException,
      MissingObjectException, IncorrectObjectTypeException, IOException,
      InvalidChangeOperationException, MergeException, NoSuchProjectException,
      UpdateException, RestApiException {
    try (BatchUpdate bu =
        updateFactory.create(db, project.getNameKey(),
            userFactory.create(pullRequestOwner), TimeUtil.nowTs())) {

      return internalAddCommitToChange(db, bu, project, repo,
          destinationBranch, pullRequestOwner, pullRequestCommit,
          pullRequestMessage, topic);
    }
  }

  public Change.Id internalAddCommitToChange(ReviewDb db, BatchUpdate bu,
      final Project project, final Repository repo,
      final String destinationBranch, final Account.Id pullRequestOwner,
      final RevCommit pullRequestCommit, final String pullRequestMesage,
      final String topic) throws InvalidChangeOperationException, IOException,
      NoSuchProjectException, OrmException, UpdateException, RestApiException {
    if (destinationBranch == null || destinationBranch.length() == 0) {
      throw new InvalidChangeOperationException(
          "Destination branch cannot be null or empty");
    }
    Ref destRef = repo.getRef(destinationBranch);
    if (destRef == null) {
      throw new InvalidChangeOperationException("Branch " + destinationBranch
          + " does not exist.");
    }

    RefControl refControl =
        projectControlFactory.controlFor(project.getNameKey()).controlForRef(
            destinationBranch);

    String pullRequestSha1 = pullRequestCommit.getId().getName();
    ResultSet<PatchSet> existingPatchSet =
        db.patchSets().byRevision(new RevId(pullRequestSha1));
    Iterator<PatchSet> patchSetIterator = existingPatchSet.iterator();
    if (patchSetIterator.hasNext()) {
      PatchSet patchSet = patchSetIterator.next();
      LOG.debug("Pull request commit ID " + pullRequestSha1
          + " has been already uploaded as PatchSetID="
          + patchSet.getPatchSetId() + " in ChangeID=" + patchSet.getId());
      return null;
    }

    Change.Key changeKey;
    final List<String> idList = pullRequestCommit.getFooterLines(CHANGE_ID);
    if (!idList.isEmpty()) {
      final String idStr = idList.get(idList.size() - 1).trim();
      changeKey = new Change.Key(idStr);
    } else {
      final ObjectId computedChangeId =
          ChangeIdUtil.computeChangeId(pullRequestCommit.getTree(),
              pullRequestCommit, pullRequestCommit.getAuthorIdent(),
              pullRequestCommit.getCommitterIdent(), pullRequestMesage);

      changeKey = new Change.Key("I" + computedChangeId.name());
    }

    String branchName = destRef.getName();
    List<ChangeData> destChanges =
        queryProvider.get().byBranchKey(
            new Branch.NameKey(project.getNameKey(),
                branchName.startsWith(REFS_HEADS)
                    ? branchName.substring(REFS_HEADS.length()) : branchName),
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
      Change destChange = destChanges.get(0).change();
      insertPatchSet(bu, repo, destChange, pullRequestCommit,
          refControl, pullRequestMesage);
      return destChange.getId();
    }

    // Change key not found on destination branch. We can create a new
    // change.
    return createNewChange(db, bu, changeKey, project.getNameKey(), destRef,
        pullRequestOwner, pullRequestCommit, refControl, pullRequestMesage,
        topic);
  }

  private void insertPatchSet(BatchUpdate bu, Repository git, Change change,
      RevCommit cherryPickCommit, RefControl refControl,
      String pullRequestMessage) throws IOException, UpdateException,
      RestApiException {
    try (RevWalk revWalk = new RevWalk(git)) {
      PatchSet.Id psId =
          ChangeUtil.nextPatchSetId(git, change.currentPatchSetId());

      PatchSetInserter patchSetInserter =
          patchSetInserterFactory.create(refControl, psId, cherryPickCommit);
      patchSetInserter.setMessage(pullRequestMessage);
      patchSetInserter.setValidatePolicy(Policy.NONE);

      bu.addOp(change.getId(), patchSetInserter);
      bu.execute();
    }
  }

  private Change.Id createNewChange(ReviewDb db, BatchUpdate bu,
      Change.Key changeKey, Project.NameKey project, Ref destRef,
      Account.Id pullRequestOwner, RevCommit pullRequestCommit,
      RefControl refControl, String pullRequestMessage, String topic)
      throws OrmException, UpdateException, RestApiException {
    Change change =
        new Change(changeKey, new Change.Id(db.nextChangeId()),
            pullRequestOwner, new Branch.NameKey(project, destRef.getName()),
            TimeUtil.nowTs());
    if (topic != null) {
      change.setTopic(topic);
    }
    ChangeInserter ins =
        changeInserterFactory.create(refControl, change, pullRequestCommit);

    ins.setMessage(pullRequestMessage);
    bu.insertChange(ins);
    bu.execute();

    return ins.getChange().getId();
  }
}
