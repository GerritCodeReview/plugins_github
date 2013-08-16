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

import com.google.gerrit.server.config.SitePath;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.github.wizard.GitCloneFailedException;
import com.googlesource.gerrit.plugins.github.wizard.GitCloner;
import com.googlesource.gerrit.plugins.github.wizard.GitConfig;
import com.googlesource.gerrit.plugins.github.wizard.GitDestinationAlreadyExistsException;
import com.googlesource.gerrit.plugins.github.wizard.GitDestinationNotWritableException;

@RunWith(JukitoRunner.class)
public class GitClonerTest {
  private static final String TEST_ORGANISATION = "lucamilanesio";
  private static final String TEST_REPOSITORY = "33degree";
  public static final File GERRIT_SITE = new File(System.getProperty(
      "gerrit.site", "/tmp/gerrit-site-test"), "git");

  public static class GitClonerTestModule extends TestModule {
    @Override
    protected void configureTest() {
      bind(GitConfig.class).toInstance(new GitConfig(GERRIT_SITE));
    }
  }

  @Inject
  private GitCloner cloner;

  @Before
  public void setUp() throws IOException {
    if (GERRIT_SITE.exists()) {
      FileUtils.deleteDirectory(GERRIT_SITE);
    }
  }

  @Test
  public void testShouldCloneBareGitHubRepositoryCreateTargetDirectoryWithGitRepository()
      throws GitDestinationAlreadyExistsException, GitCloneFailedException,
      GitDestinationNotWritableException {
    cloner.clone(TEST_ORGANISATION, TEST_REPOSITORY);

    File destDirectory =
        new File(new File(GERRIT_SITE, TEST_ORGANISATION), TEST_REPOSITORY + ".git");
    assertTrue(destDirectory.exists());
    assertTrue(new File(destDirectory, "HEAD").exists());
  }

}
