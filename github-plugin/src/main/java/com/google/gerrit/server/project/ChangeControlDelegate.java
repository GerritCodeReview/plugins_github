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
package com.google.gerrit.server.project;

import java.util.List;

import com.google.gerrit.common.data.LabelTypes;
import com.google.gerrit.common.data.PermissionRange;
import com.google.gerrit.common.data.SubmitRecord;
import com.google.gerrit.common.data.SubmitTypeRecord;
import com.google.gerrit.reviewdb.client.Account.Id;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.reviewdb.client.PatchSet;
import com.google.gerrit.reviewdb.client.PatchSetApproval;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.query.change.ChangeData;
import com.google.gwtorm.server.OrmException;
import com.googlecode.prolog_cafe.lang.Term;

public class ChangeControlDelegate extends ChangeControl {

  private final ChangeControl realChangeControl;
  private final CurrentUser delegatedUser;

  private ChangeControlDelegate(ChangeControl realChangeControl, CurrentUser delegatedUser) {
    // All methods are delegated, super class constructor needs to be invoked anyway with dummy values
    super(null, null, null, null);
    this.realChangeControl = realChangeControl;
    this.delegatedUser = delegatedUser;
  }

  public static ChangeControl wrap(ChangeControl realChangeControl, CurrentUser delegatedUser) {
    return new ChangeControlDelegate(realChangeControl, delegatedUser);
  }

  public int hashCode() {
    return realChangeControl.hashCode();
  }

  public boolean equals(Object obj) {
    return realChangeControl.equals(obj);
  }

  public ChangeControl forUser(CurrentUser who) {
    return realChangeControl.forUser(who);
  }

  public RefControl getRefControl() {
    return realChangeControl.getRefControl();
  }

  public CurrentUser getCurrentUser() {
    return delegatedUser;
  }

  public ProjectControl getProjectControl() {
    return realChangeControl.getProjectControl();
  }

  public Project getProject() {
    return realChangeControl.getProject();
  }

  public Change getChange() {
    return realChangeControl.getChange();
  }

  public boolean isVisible(ReviewDb db) throws OrmException {
    return realChangeControl.isVisible(db);
  }

  public boolean isRefVisible() {
    return realChangeControl.isRefVisible();
  }

  public boolean isPatchVisible(PatchSet ps, ReviewDb db) throws OrmException {
    return realChangeControl.isPatchVisible(ps, db);
  }

  public boolean canAbandon() {
    return realChangeControl.canAbandon();
  }

  public boolean canPublish(ReviewDb db) throws OrmException {
    return realChangeControl.canPublish(db);
  }

  public boolean canDeleteDraft(ReviewDb db) throws OrmException {
    return realChangeControl.canDeleteDraft(db);
  }

  public boolean canRebase() {
    return realChangeControl.canRebase();
  }

  public boolean canRestore() {
    return realChangeControl.canRestore();
  }

  public LabelTypes getLabelTypes() {
    return realChangeControl.getLabelTypes();
  }

  public String toString() {
    return realChangeControl.toString();
  }

  public List<PermissionRange> getLabelRanges() {
    return realChangeControl.getLabelRanges();
  }

  public PermissionRange getRange(String permission) {
    return realChangeControl.getRange(permission);
  }

  public boolean canAddPatchSet() {
    return realChangeControl.canAddPatchSet();
  }

  public boolean isOwner() {
    return realChangeControl.isOwner();
  }

  public boolean isReviewer(ReviewDb db) throws OrmException {
    return realChangeControl.isReviewer(db);
  }

  public boolean isReviewer(ReviewDb db, ChangeData cd) throws OrmException {
    return realChangeControl.isReviewer(db, cd);
  }

  public boolean canRemoveReviewer(PatchSetApproval approval) {
    return realChangeControl.canRemoveReviewer(approval);
  }

  public boolean canRemoveReviewer(Id reviewer, int value) {
    return realChangeControl.canRemoveReviewer(reviewer, value);
  }

  public boolean canEditTopicName() {
    return realChangeControl.canEditTopicName();
  }

  public List<SubmitRecord> getSubmitRecords(ReviewDb db, PatchSet patchSet) {
    return realChangeControl.getSubmitRecords(db, patchSet);
  }

  public boolean canSubmit() {
    return realChangeControl.canSubmit();
  }

  public List<SubmitRecord> canSubmit(ReviewDb db, PatchSet patchSet) {
    return realChangeControl.canSubmit(db, patchSet);
  }

  public List<SubmitRecord> canSubmit(ReviewDb db, PatchSet patchSet,
      ChangeData cd, boolean fastEvalLabels, boolean allowClosed,
      boolean allowDraft) {
    return realChangeControl.canSubmit(db, patchSet, cd, fastEvalLabels,
        allowClosed, allowDraft);
  }

  public List<SubmitRecord> resultsToSubmitRecord(Term submitRule,
      List<Term> results) {
    return realChangeControl.resultsToSubmitRecord(submitRule, results);
  }

  public SubmitTypeRecord getSubmitTypeRecord(ReviewDb db, PatchSet patchSet) {
    return realChangeControl.getSubmitTypeRecord(db, patchSet);
  }

  public SubmitTypeRecord getSubmitTypeRecord(ReviewDb db, PatchSet patchSet,
      ChangeData cd) {
    return realChangeControl.getSubmitTypeRecord(db, patchSet, cd);
  }
}
