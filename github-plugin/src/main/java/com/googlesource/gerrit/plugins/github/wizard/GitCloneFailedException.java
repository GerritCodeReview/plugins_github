package com.googlesource.gerrit.plugins.github.wizard;

public class GitCloneFailedException extends Exception {
  public GitCloneFailedException(String remoteUrl, Exception e) {
    super("Failed to clone from repository " + remoteUrl, e);
  }

  private static final long serialVersionUID = 1619949108894445899L;

}
