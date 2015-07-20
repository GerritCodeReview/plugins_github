package com.googlesource.gerrit.plugins.github.notification;

import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.kohsuke.github.GHEventPayload.PullRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.httpd.WebSession;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.reviewdb.client.Account.Id;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.AccountVisibilityProvider;
import com.google.gerrit.server.util.ThreadLocalRequestContext;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.servlet.RequestScoped;
import com.google.inject.servlet.SessionScoped;
import com.googlesource.gerrit.plugins.github.git.JobExecutor;
import com.googlesource.gerrit.plugins.github.git.PullRequestImportJob;
import com.googlesource.gerrit.plugins.github.git.PullRequestImportType;
import com.googlesource.gerrit.plugins.github.git.PullRequestImporter;

import org.apache.lucene.analysis.standard.StandardTokenizerInterface;
import org.eclipse.egit.github.core.service.OrganizationService;
import org.kohsuke.github.GHRepository;

class PullRequestHandler implements EventHandler<PullRequest> {
  private static final Logger logger =
      LoggerFactory.getLogger(PullRequestHandler.class);
  private final JobExecutor jobExecutor;
  private final PullRequestImportJob.Factory jobFactory;
  private final IdentifiedUser.GenericFactory userFactory;
  private final DynamicItem<WebSession> session;

  // private final Provider<PullRequestImporter> prImportProvider;

  @Inject
  public PullRequestHandler(
      // final Provider<PullRequestImporter> pullRequestsImporter,
      final JobExecutor jobExecutor,
      final PullRequestImportJob.Factory jobFactory,
      final IdentifiedUser.GenericFactory userFactory,
      final DynamicItem<WebSession> session) {
    // this.prImportProvider = pullRequestsImporter;
    this.jobExecutor = jobExecutor;
    this.jobFactory = jobFactory;
    this.userFactory = userFactory;
    this.session = session;
  }

  @Override
  public void doAction(PullRequest payload, HttpServletRequest req,
      HttpServletResponse resp) throws IOException {
    Id id = Account.Id.fromRef("yugui-gerrit-example");
    IdentifiedUser user = userFactory.create(id);
    PullRequestImporter prImporter =
        new PullRequestImporter(jobExecutor, user, jobFactory);
  //  session.get().setUserAccountId(id);

    final String action = payload.getAction();
    if (action.equals("opened") || action.equals("synchronize")) {
      final GHRepository repository = payload.getRepository();
      final int prNumber = payload.getNumber();
      // PullRequestImporter prImporter = prImportProvider.get();

      final String organization = repository.getOwnerName();
      final String name = repository.getName(); 
      logger.info("owner={}, name={}, number={}", organization, name, prNumber);
      prImporter.importPullRequest(0, organization, name, 
          prNumber, PullRequestImportType.Commits);
    }
    resp.setStatus(SC_NO_CONTENT);
  }

  @Override
  public Class<PullRequest> getPayloadType() {
    return PullRequest.class;
  }
}
