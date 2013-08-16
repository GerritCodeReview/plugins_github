package com.googlesource.gerrit.plugins.github.wizard;

import java.io.File;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class GitCloner {
  private static final String GITHUB_REPOSITORY_FORMAT =
      "https://github.com/%1$s/%2$s.git";
  private static final Logger log = LoggerFactory.getLogger(GitCloner.class);
  private final File gitDir;

  @Inject
  public GitCloner(GitConfig gitConfig) {
    gitDir = gitConfig.gitDir;
  }

  public void clone(String organisation, String repository)
      throws GitCloneFailedException, GitDestinationAlreadyExistsException,
      GitDestinationNotWritableException {
    CloneCommand clone = new CloneCommand();
    final String sourceUri = getSourceUri(organisation, repository);
    clone.setURI(sourceUri);
    clone.setBare(true);
    final File destinationDirectory = getDestinationDirectory(organisation, repository);
    clone.setDirectory(destinationDirectory);
    clone.setProgressMonitor(new ProgressMonitor() {

      private int totTasks;
      private int currTask;
      private int totUnits;
      private int currUnit;
      private int lastPercentage;

      @Override
      public void update(int completed) {
        if(totUnits == 0) {
          return;
        }
        
        currUnit += completed;
        int percentage = currUnit * 10 / totUnits;
        if (percentage > lastPercentage) {
          log.info(sourceUri + "| " + percentage + "0% done");
          lastPercentage = percentage;
        }
      }

      @Override
      public void start(int totalTasks) {
        log.info(sourceUri + "| Starting " + totalTasks + " tasks");
        totTasks = totalTasks;
        currTask = 0;
      }

      @Override
      public boolean isCancelled() {
        return false;
      }

      @Override
      public void endTask() {
      }

      @Override
      public void beginTask(String task, int totalUnits) {
        currTask++;
        totUnits = totalUnits;
        currUnit = 0;
        lastPercentage = 0;
        log.info(sourceUri + "| Task " + currTask + "/" + totTasks + ": " + task);
      }
    });
    try {
      log.info(sourceUri + "| Clone into " + destinationDirectory);
      clone.call();
    } catch (Exception e) {
      throw new GitCloneFailedException(sourceUri, e);
    }
  }

  private File getDestinationDirectory(String organisation, String repository)
      throws GitDestinationAlreadyExistsException,
      GitDestinationNotWritableException {
    File destDirectory =
        new File(new File(gitDir, organisation), repository + ".git");
    if (destDirectory.exists() && isNotEmpty(destDirectory)) {
      throw new GitDestinationAlreadyExistsException(destDirectory);
    }

    if (!destDirectory.exists()) {
      if (!destDirectory.mkdirs()) {
        throw new GitDestinationNotWritableException(destDirectory);
      }
    }

    return destDirectory;
  }

  private boolean isNotEmpty(File destDirectory) {
    return destDirectory.listFiles().length > 0;
  }

  private String getSourceUri(String organisation, String repository) {
    return String.format(GITHUB_REPOSITORY_FORMAT, organisation, repository);
  }

}
