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

import org.eclipse.jgit.lib.ProgressMonitor;

public class GitImportJob extends AbstractCloneJob implements Runnable,
    ProgressMonitor, GitJob {
  private int currTask;
  private int totUnits;
  private int currUnit;
  private int lastPercentage;
  private boolean cancelled;
  private String task = "Initializing ...";
  private Exception exception;
  private GitJobStatus status = GitJobStatus.SYNC;
  private int index;
  private final ImportStep[] importSteps;
  private String organisation;
  private String repository;

  public GitImportJob(int id, String organisation, String repository, ImportStep... steps) {
    this.importSteps = steps;
    this.index = id;
    this.organisation = organisation;
    this.repository = repository;
  }

  @Override
  public void run() {
    try {
      for (ImportStep importStep : importSteps) {
        importStep.doImport(this);
      }
      status = GitJobStatus.COMPLETE;
    } catch (Exception e) {
      if (status == GitJobStatus.SYNC) {
        this.exception = e;
        status = GitJobStatus.FAILED;
      }
      rollback();
    }
  }

  private void rollback() {
    for (ImportStep importStep : importSteps) {
      importStep.rollback();
    }
  }

  @Override
  public void cancel() {
    if (status != GitJobStatus.SYNC) {
      return;
    }

    cancelled = true;
    status = GitJobStatus.CANCELLED;
    rollback();
  }



  /*
   * (non-Javadoc)
   * 
   * @see
   * com.googlesrouce.gerrit.plugins.github.git.CloneJob#getStatusDescription()
   */
  @Override
  public String getStatusDescription() {
    if (exception != null) {
      return getErrorDescription(exception);
    } else {
      switch (status) {
        case COMPLETE:
          return "Cloned (100%)";
        case CANCELLED:
          return "Cancelled";
        default:
          return "Phase-" + currTask + " / " + task + " (" + lastPercentage
              + "%)";
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.googlesrouce.gerrit.plugins.github.git.CloneJob#getStatus()
   */
  @Override
  public GitJobStatus getStatus() {
    return status;
  }

  @Override
  public void update(int completed) {
    if (totUnits == 0) {
      return;
    }

    currUnit += completed;
    int percentage = currUnit * 100 / totUnits;
    if (percentage > lastPercentage) {
      lastPercentage = percentage;
    }
  }

  @Override
  public void start(int totalTasks) {
    currTask = 0;
  }

  @Override
  public boolean isCancelled() {
    if (cancelled) {
      status = GitJobStatus.CANCELLED;
    }
    return cancelled;
  }

  @Override
  public void endTask() {
  }

  @Override
  public void beginTask(String task, int totalUnits) {
    this.currTask++;
    this.task = task;
    this.totUnits = totalUnits;
    this.currUnit = 0;
    this.lastPercentage = 0;
  }

  @Override
  public int getIndex() {
    return index;
  }

  @Override
  public String toString() {
    return "CloneJob#" + index + " " + getOrganisation() + "/"
        + getRepository();
  }

  @Override
  public String getOrganisation() {
    return organisation;
  }

  @Override
  public String getRepository() {
    return repository;
  }

}
