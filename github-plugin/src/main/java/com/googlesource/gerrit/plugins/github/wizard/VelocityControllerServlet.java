// Copyright (C) 2013 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.googlesource.gerrit.plugins.github.wizard;

import com.google.gerrit.server.IdentifiedUser;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import com.googlesource.gerrit.plugins.github.GitHubConfig;
import com.googlesource.gerrit.plugins.github.GitHubConfig.NextPage;
import com.googlesource.gerrit.plugins.github.oauth.GitHubLogin;
import com.googlesource.gerrit.plugins.github.oauth.ScopedProvider;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Singleton
public class VelocityControllerServlet extends HttpServlet {
  private static final long serialVersionUID = 5565594120346641704L;
  private static final Logger log = LoggerFactory
      .getLogger(VelocityControllerServlet.class);
  private static final String CONTROLLER_PACKAGE =
      VelocityControllerServlet.class.getPackage().getName();
  private final ScopedProvider<GitHubLogin> loginProvider;
  private final Provider<IdentifiedUser> userProvider;
  private final Injector injector;
  private final Provider<ControllerErrors> errorsProvider;
  private final GitHubConfig githubConfig;

  @Inject
  public VelocityControllerServlet(final ScopedProvider<GitHubLogin> loginProvider,
      Provider<IdentifiedUser> userProvider, final Injector injector,
      Provider<ControllerErrors> errorsProvider, GitHubConfig githubConfig) {
    this.loginProvider = loginProvider;
    this.userProvider = userProvider;
    this.injector = injector;
    this.errorsProvider = errorsProvider;
    this.githubConfig = githubConfig;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String controllerName;
    VelocityController controller;
    controllerName = getControllerClassName(req);

    try {
      Class<? extends VelocityController> controllerClass =
          (Class<? extends VelocityController>) Class
              .forName(CONTROLLER_PACKAGE + "." + controllerName + "Controller");
      controller = injector.getInstance(controllerClass);
    } catch (ClassNotFoundException e) {
      log.debug("Cannot find any controller for servlet "
          + req.getServletPath());
      redirectToNextStep(req, resp);
      return;
    }

    GitHubLogin hubLogin = loginProvider.get(req);
    IdentifiedUser user = userProvider.get();
    WrappedResponse wrappedResp = new WrappedResponse(resp);
    controller.doAction(user, hubLogin, req, wrappedResp, errorsProvider.get());

    if (wrappedResp.getStatus() == 0 ||
        wrappedResp.getStatus() == HttpStatus.SC_OK) {
      redirectToNextStep(req, resp);
    }
  }

  private String getControllerClassName(HttpServletRequest req) {
    String reqServletName;
    StringBuilder controllerName = new StringBuilder();
    reqServletName = req.getPathInfo();
    reqServletName = trimFromChar(reqServletName, '/');
    reqServletName = trimUpToChar(reqServletName, '.');
    String[] controllerNameParts = reqServletName.split("-");

    for (String namePart : controllerNameParts) {
      controllerName.append(Character.toUpperCase(namePart.charAt(0))
          + namePart.substring(1));
    }
    return controllerName.toString();
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
      HttpServletResponse resp) throws IOException, ServletException {
    String sourceUri = req.getRequestURI();
    int pathPos = sourceUri.lastIndexOf('/') + 1;
    String sourcePage = sourceUri.substring(pathPos);
    String sourcePath = sourceUri.substring(0, pathPos);
    int queryStringStart = sourcePage.indexOf('?');
    if (queryStringStart > 0) {
      sourcePage = sourcePage.substring(0, queryStringStart);
    }
    NextPage nextPage = githubConfig.getNextPage(sourcePage);
    if (nextPage != null) {
      if (nextPage.redirect) {
        resp.sendRedirect(nextPageURL(sourcePath, nextPage));
      } else {
        RequestDispatcher requestDispatcher =
            req.getRequestDispatcher(nextPageURL(sourcePath, nextPage));
        req.setAttribute("destUrl", nextPage);
        requestDispatcher.forward(req, resp);
      }
    }
  }

  private String nextPageURL(String sourcePath, NextPage nextPage) {
    return nextPage.uri.startsWith("/") ? nextPage.uri
        : sourcePath + nextPage.uri;
  }
}
