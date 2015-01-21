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

import com.google.gerrit.common.TimeUtil;
import com.google.gerrit.common.errors.EmailException;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.reviewdb.client.Branch;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.reviewdb.client.Change.Id;
import com.google.gerrit.reviewdb.client.ChangeMessage;
import com.google.gerrit.reviewdb.client.PatchSet;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.reviewdb.client.RevId;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.ChangeUtil;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.IdentifiedUser.GenericFactory;
import com.google.gerrit.server.change.ChangeInserter;
import com.google.gerrit.server.change.PatchSetInserter;
import com.google.gerrit.server.change.PatchSetInserter.ValidatePolicy;
import com.google.gerrit.server.events.CommitReceivedEvent;
import com.google.gerrit.server.git.MergeException;
import com.google.gerrit.server.git.validators.CommitValidationException;
import com.google.gerrit.server.git.validators.CommitValidators;
import com.google.gerrit.server.project.ChangeControl;
import com.google.gerrit.server.project.InvalidChangeOperationException;
import com.google.gerrit.server.project.NoSuchChangeException;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.gerrit.server.project.ProjectControl;
import com.google.gerrit.server.project.RefControl;
import com.google.gerrit.server.ssh.NoSshInfo;
import com.google.gwtorm.server.OrmException;
import com.google.gwtorm.server.ResultSet;
import com.google.inject.Inject;

import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.FooterKey;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.ReceiveCommand;
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

  private final IdentifiedUser currentUser;
  private final CommitValidators.Factory commitValidatorsFactory;
  private final ChangeInserter.Factory changeInserterFactory;
  private final PatchSetInserter.Factory patchSetInserterFactory;
  private final ProjectControl.Factory projectControlFactory;
  private final GenericFactory userFactory;


  @Inject
  PullRequestCreateChange(final IdentifiedUser currentUser,
      final CommitValidators.Factory commitValidatorsFactory,
      final ChangeInserter.Factory changeInserterFactory,
      final PatchSetInserter.Factory patchSetInserterFactory,
      final ProjectControl.Factory projectControlFactory,
      final IdentifiedUser.GenericFactory userFactory) {
    this.currentUser = currentUser;
    this.commitValidatorsFactory = commitValidatorsFactory;
    this.changeInserterFactory = changeInserterFactory;
    this.patchSetInserterFactory = patchSetInserterFactory;
    this.projectControlFactory = projectControlFactory;
    this.userFactory = userFactory;
  }

  public Change.Id addCommitToChange(final ReviewDb db, final Project project,
      final Repository git, final String destinationBranch,
      final Account.Id pullRequestOwner, final RevCommit pullRequestCommit,
      final String pullRequestMesage, final String topic, boolean doValidation)
      throws NoSuchChangeException, EmailException, OrmException,
      MissingObjectException, IncorrectObjectTypeException, IOException,
      InvalidChangeOperationException, MergeException, NoSuchProjectException {
    Id newChange = null;
    if (destinationBranch == null || destinationBranch.length() == 0) {
      throw new InvalidChangeOperationException(
          "Destination branch cannot be null or empty");
    }

    RefControl refControl =
        projectControlFactory.controlFor(project.getNameKey()).controlForRef(
            destinationBranch);

    try {
      RevWalk revWalk = new RevWalk(git);
      try {
        Ref destRef = git.getRef(destinationBranch);
        if (destRef == null) {
          throw new InvalidChangeOperationException("Branch "
              + destinationBranch + " does not exist.");
        }

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

        List<Change> destChanges =
            db.changes()
                .byBranchKey(
                    new Branch.NameKey(project.getNameKey(), destRef.getName()),
                    changeKey).toList();

        if (destChanges.size() > 1) {
          throw new InvalidChangeOperationException(
              "Multiple Changes with Change-ID "
                  + changeKey
                  + " already exist on the target branch: cannot add a new patch-set "
                  + destinationBranch);
        } else if (destChanges.size() == 1) {
          // The change key exists on the destination branch: adding a new
          // patch-set
          Change destChange = destChanges.get(0);

          ChangeControl changeControl =
              projectControlFactory.controlFor(project.getNameKey()).controlFor(
                  destChange).forUser(userFactory.create(pullRequestOwner));

          return insertPatchSet(git, revWalk, destChange, pullRequestCommit,
              changeControl, pullRequestOwner, pullRequestMesage, doValidation);
        } else {
          // Change key not found on destination branch. We can create a new
          // change.
          return (newChange =
              createNewChange(db, git, revWalk, changeKey,
                  project.getNameKey(), destRef, pullRequestOwner,
                  pullRequestCommit, refControl, pullRequestMesage, topic,
                  doValidation));
        }
      } finally {
        revWalk.release();
        if (newChange == null) {
          db.rollback();
        }
      }
    } finally {
      git.close();
    }
  }

  private Change.Id insertPatchSet(Repository git, RevWalk revWalk,
      Change change, RevCommit cherryPickCommit, ChangeControl changeControl,
      Account.Id pullRequestOwnerId, String pullRequestMessage,
      boolean doValidation) throws InvalidChangeOperationException,
      IOException, OrmException, NoSuchChangeException {
    PatchSetInserter patchSetInserter =
        patchSetInserterFactory.create(git, revWalk, changeControl, cherryPickCommit);
    // This apparently useless method call is made for triggering
    // the creation of patchSet inside PatchSetInserter and thus avoiding a NPE
    patchSetInserter.getPatchSetId();
    patchSetInserter.setMessage(pullRequestMessage);

    patchSetInserter.setValidatePolicy(doValidation ? ValidatePolicy.GERRIT
        : ValidatePolicy.NONE);
    patchSetInserter.insert();
    return change.getId();
  }

  private Change.Id createNewChange(ReviewDb db, Repository git,
      RevWalk revWalk, Change.Key changeKey, Project.NameKey project,
      Ref destRef, Account.Id pullRequestOwner, RevCommit pullRequestCommit,
      RefControl refControl, String pullRequestMessage, String topic,
      boolean doValidation) throws OrmException,
      InvalidChangeOperationException, IOException {
    Change change =
        new Change(changeKey, new Change.Id(db.nextChangeId()),
            pullRequestOwner, new Branch.NameKey(project, destRef.getName()),
            TimeUtil.nowTs());
    if (topic != null) {
      change.setTopic(topic);
    }
    ChangeInserter ins =
        changeInserterFactory.create(refControl, change, pullRequestCommit);
    PatchSet newPatchSet = ins.getPatchSet();

    if (doValidation) {
      validate(git, pullRequestCommit, refControl, newPatchSet);
    }

    final RefUpdate ru = git.updateRef(newPatchSet.getRefName());
    ru.setExpectedOldObjectId(ObjectId.zeroId());
    ru.setNewObjectId(pullRequestCommit);
    ru.disableRefLog();
    if (ru.update(revWalk) != RefUpdate.Result.NEW) {
      throw new IOException(String.format("Failed to create ref %s in %s: %s",
          newPatchSet.getRefName(), change.getDest().getParentKey().get(),
          ru.getResult()));
    }

    ins.setMessage(
        buildChangeMessage(db, change, newPatchSet, pullRequestOwner, pullRequestMessage))
        .insert();

    return change.getId();
  }

  private void validate(Repository git, RevCommit pullRequestCommit,
      RefControl refControl, PatchSet newPatchSet)
      throws InvalidChangeOperationException {
    CommitValidators commitValidators =
        commitValidatorsFactory.create(refControl, new NoSshInfo(), git);
    CommitReceivedEvent commitReceivedEvent =
        new CommitReceivedEvent(new ReceiveCommand(ObjectId.zeroId(),
            pullRequestCommit.getId(), newPatchSet.getRefName()), refControl
            .getProjectControl().getProject(), refControl.getRefName(),
            pullRequestCommit, currentUser);

    try {
      commitValidators.validateForGerritCommits(commitReceivedEvent);
    } catch (CommitValidationException e) {
      throw new InvalidChangeOperationException(e.getMessage());
    }
  }

  private ChangeMessage buildChangeMessage(ReviewDb db, Change dest,
      PatchSet newPatchSet, Account.Id pullRequestAuthorId,
      String pullRequestMessage) throws OrmException {
    ChangeMessage cmsg =
        new ChangeMessage(new ChangeMessage.Key(dest.getId(),
            ChangeUtil.messageUUID(db)), pullRequestAuthorId, TimeUtil.nowTs(),
            newPatchSet.getId());
    cmsg.setMessage(pullRequestMessage);
    return cmsg;
  }
}
