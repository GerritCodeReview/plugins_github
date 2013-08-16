package com.googlesource.gerrit.plugins.github.wizard;

import java.io.File;

public class GitDestinationNotWritableException extends Exception {
  private static final long serialVersionUID = -6486633812790391401L;

  public GitDestinationNotWritableException(File destDirectory) {
    super("Destination Git directory " + destDirectory + " is not writable");
  }

}
