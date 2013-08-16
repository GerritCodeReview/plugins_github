package com.googlesource.gerrit.plugins.github.wizard;

import java.io.File;

import org.eclipse.jgit.lib.Config;

import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.config.SitePaths;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class GitConfig {

  public final File gitDir;

  public GitConfig(File gitDir) {
    this.gitDir = gitDir;
  }

  @Inject
  public GitConfig(final SitePaths site, @GerritServerConfig final Config cfg) {
    gitDir = site.resolve(cfg.getString("gerrit", null, "basePath"));
    if (gitDir == null) {
      throw new IllegalStateException("gerrit.basePath must be configured");
    }
  }
}
