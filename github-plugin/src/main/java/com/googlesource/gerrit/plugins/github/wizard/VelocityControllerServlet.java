package com.googlesource.gerrit.plugins.github.wizard;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.gerrit.server.IdentifiedUser;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.github.oauth.GitHubLogin;

@Singleton
public class VelocityControllerServlet extends HttpServlet {
  private static final long serialVersionUID = 5565594120346641704L;
  private static final Logger log = LoggerFactory
      .getLogger(VelocityControllerServlet.class);
  private static final String CONTROLLER_PACKAGE =
      VelocityControllerServlet.class.getPackage().getName();
  private final Provider<GitHubLogin> loginProvider;
  private final Provider<IdentifiedUser> userProvider;
  private final Injector injector;

  @Inject
  public VelocityControllerServlet(final Provider<GitHubLogin> loginProvider,
      Provider<IdentifiedUser> userProvider, final Injector injector) {
    this.loginProvider = loginProvider;
    this.userProvider = userProvider;
    this.injector = injector;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String controllerName = req.getServletPath();
    VelocityController controller;

    controllerName = trimFromChar(controllerName, '/');
    controllerName = trimUpToChar(controllerName, '.');
    controllerName =
        Character.toUpperCase(controllerName.charAt(0))
            + controllerName.substring(1);

    try {
      Class<? extends VelocityController> controllerClass =
          (Class<? extends VelocityController>) Class
              .forName(CONTROLLER_PACKAGE + "." + controllerName + "Controller");
      controller = injector.getInstance(controllerClass);
    } catch (ClassNotFoundException e) {
      log.error("Cannot find any controller for servlet "
          + req.getServletPath());
      resp.sendError(HttpStatus.SC_NOT_FOUND);
      return;
    }

    GitHubLogin hubLogin = loginProvider.get();
    IdentifiedUser user = userProvider.get();
    WrappedResponse wrappedResp = new WrappedResponse(resp);
    controller.doAction(user, hubLogin, req, wrappedResp);

    if (wrappedResp.getStatus() == HttpStatus.SC_OK) {
      redirectToNextStep(req, resp);
    }
  }

  private String trimUpToChar(String string, char ch) {
    if (string.indexOf(ch) >= 0) {
      string = string.substring(0, string.indexOf(ch));
    }
    return string;
  }

  private String trimFromChar(String string, char ch) {
    if (string.lastIndexOf(ch) >= 0) {
      string = string.substring(string.lastIndexOf(ch) + 1);
    }
    return string;
  }

  private void redirectToNextStep(HttpServletRequest req,
      HttpServletResponse resp) throws IOException {
    String nextUrl = req.getParameter("next");
    if (Strings.emptyToNull(nextUrl) != null) {
      resp.sendRedirect(nextUrl);
    }
  }
}
