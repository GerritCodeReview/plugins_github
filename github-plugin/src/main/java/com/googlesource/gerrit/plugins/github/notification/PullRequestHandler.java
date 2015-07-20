package com.googlesource.gerrit.plugins.github.notification;

import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.kohsuke.github.GHEventPayload.PullRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.googlesource.gerrit.plugins.github.git.PullRequestImportType;
import com.googlesource.gerrit.plugins.github.git.PullRequestImporter;

import org.kohsuke.github.GHRepository;

class PullRequestHandler implements EventHandler<PullRequest> {
  private static final Logger logger =
      LoggerFactory.getLogger(PullRequestHandler.class);
  private final Provider<PullRequestImporter> prImportProvider;

  @Inject
  public PullRequestHandler(
      final Provider<PullRequestImporter> pullRequestsImporter) {
    this.prImportProvider = pullRequestsImporter;
  }

  @Override
  public void doAction(PullRequest payload, HttpServletRequest req,
      HttpServletResponse resp) throws IOException {
    final String action = payload.getAction();
    if (action.equals("opened") || action.equals("synchronize")) {
      final GHRepository repository = payload.getRepository();
      final int prNumber = payload.getNumber();
      final PullRequestImporter prImporter = prImportProvider.get();
      final String organization = repository.getOwnerName();
      final String name = repository.getName();
      logger.info("Importing {}/{}#{}", organization, name, prNumber);
      prImporter.importPullRequest(0, organization, name, prNumber,
          PullRequestImportType.Commits);
      logger.info("Imported {}/{}#{}", organization, name, prNumber);
    }
    resp.setStatus(SC_NO_CONTENT);
  }

  @Override
  public Class<PullRequest> getPayloadType() {
    return PullRequest.class;
  }
}
