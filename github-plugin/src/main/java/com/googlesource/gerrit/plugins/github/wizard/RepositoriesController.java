package com.googlesource.gerrit.plugins.github.wizard;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gerrit.server.IdentifiedUser;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.github.oauth.GitHubLogin;

public class RepositoriesController implements VelocityController {
  private static final String REPO_PARAM_PREFIX = "repo_check.";
  private final GitCloner gitCloner;

  @Inject
  public RepositoriesController(GitCloner gitCloner) {
    this.gitCloner = gitCloner;
  }

  @Override
  public void doAction(IdentifiedUser user, GitHubLogin hubLogin,
      HttpServletRequest req, HttpServletResponse resp,
      ControllerErrors errorMgr) throws ServletException, IOException {

    Set<Entry<String, String[]>> params = req.getParameterMap().entrySet();
    for (Entry<String, String[]> param : params) {
      String paramName = param.getKey();
      String[] paramValue = param.getValue();
      if (paramName.startsWith(REPO_PARAM_PREFIX) && paramValue.length == 1
          && paramValue[0].equalsIgnoreCase("on")) {
        String orgAndRepo = paramName.substring(REPO_PARAM_PREFIX.length());
        String organisation = orgAndRepo.substring(0, orgAndRepo.indexOf('.'));
        String repository = orgAndRepo.substring(orgAndRepo.indexOf('.') + 1);
        try {
          gitCloner.clone(organisation, repository);
        } catch (Exception e) {
          errorMgr.submit(e);
        }
      }
    }
  }

}
