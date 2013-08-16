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

public class GitCloneJob extends AbstractCloneJob implements Runnable,
    ProgressMonitor, CloneJob {
  private GitClone cloneCommand;


  private int currTask;
  private int totUnits;
  private int currUnit;
  private int lastPercentage;
  private boolean cancelled;
  private String task = "Initializing ...";
  private Exception exception;
  private GitCloneStatus status = GitCloneStatus.SYNC;
  private int index;

  public GitCloneJob(int id, GitClone cloneCommand) {
    this.cloneCommand = cloneCommand;
    this.index = id;
  }

  @Override
  public void run() {
    try {
      cloneCommand.doClone(this);
      status = GitCloneStatus.COMPLETE;
    } catch (Exception e) {
      if (status == GitCloneStatus.SYNC) {
        this.exception = e;
        status = GitCloneStatus.FAILED;
      }
      cloneCommand.cleanUp();
    }
  }

  @Override
  public void cancel() {
    if (status != GitCloneStatus.SYNC) {
      return;
    }

    cancelled = true;
    status = GitCloneStatus.CANCELLED;
    cloneCommand.cleanUp();
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
  public GitCloneStatus getStatus() {
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
      status = GitCloneStatus.CANCELLED;
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

  public GitClone getCloneCommand() {
    return cloneCommand;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.googlesrouce.gerrit.plugins.github.git.CloneJob#getIndex()
   */
  @Override
  public int getIndex() {
    return index;
  }

  @Override
  public String getOrganisation() {
    return cloneCommand.getOrganisation();
  }

  @Override
  public String getRepository() {
    return cloneCommand.getRepository();
  }

  @Override
  public String toString() {
    return "CloneJob#" + index + " " + getOrganisation() + "/"
        + getRepository();
  }

}
