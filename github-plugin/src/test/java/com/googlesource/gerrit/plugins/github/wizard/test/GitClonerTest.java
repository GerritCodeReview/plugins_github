package com.googlesource.gerrit.plugins.github.wizard.test;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.jukito.JukitoRunner;
import org.jukito.TestModule;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.SitePath;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.github.GuiceModule;
import com.googlesrouce.gerrit.plugins.github.git.GitClone;
import com.googlesrouce.gerrit.plugins.github.git.GitCloneFailedException;
import com.googlesrouce.gerrit.plugins.github.git.GitConfig;
import com.googlesrouce.gerrit.plugins.github.git.GitDestinationAlreadyExistsException;
import com.googlesrouce.gerrit.plugins.github.git.GitDestinationNotWritableException;

@RunWith(JukitoRunner.class)
public class GitClonerTest {
  private static final String GERRIT_SITE_PATH = System.getProperty(
      "gerrit.site", "/tmp/gerrit-site-test");
  private static final String TEST_ORGANISATION = "lucamilanesio";
  private static final String TEST_REPOSITORY = "33degree";
  public static final File GIT_BASE_PATH = new File(GERRIT_SITE_PATH, "git");

  public static class GitClonerTestModule extends TestModule {
    @Override
    protected void configureTest() {
      bind(GitConfig.class).toInstance(new GitConfig(GIT_BASE_PATH));
      bind(File.class).annotatedWith(SitePath.class).toInstance(
          new File(GERRIT_SITE_PATH));
      bind(String.class).annotatedWith(PluginName.class).toInstance("TEST");
      install(new GuiceModule());
    }
  }

  @Inject
  private GitClone.Factory cloner;

  @Before
  public void setUp() throws IOException {
    if (GIT_BASE_PATH.exists()) {
      FileUtils.deleteDirectory(GIT_BASE_PATH);
    }
  }

  @Test
  public void testShouldCloneBareGitHubRepositoryCreateTargetDirectoryWithGitRepository()
      throws GitDestinationAlreadyExistsException, GitCloneFailedException,
      GitDestinationNotWritableException {
    cloner.create(TEST_ORGANISATION, TEST_REPOSITORY).doClone(null);

    File destDirectory =
        new File(new File(GIT_BASE_PATH, TEST_ORGANISATION), TEST_REPOSITORY
            + ".git");
    assertTrue(destDirectory.exists());
    assertTrue(new File(destDirectory, "HEAD").exists());
  }

}
