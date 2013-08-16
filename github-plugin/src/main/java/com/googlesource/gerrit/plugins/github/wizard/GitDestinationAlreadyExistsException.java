package com.googlesource.gerrit.plugins.github.wizard;

import java.io.File;
import java.io.IOException;

public class GitDestinationAlreadyExistsException extends IOException {
  private static final long serialVersionUID = -6202681486717426148L;

  public GitDestinationAlreadyExistsException(File destDirectory) {
    super("Output Git destination " + destDirectory
        + " already exists and cannot be overwritten");
  }
}
