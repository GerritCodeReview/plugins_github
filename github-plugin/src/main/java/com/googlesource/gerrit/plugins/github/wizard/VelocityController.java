package com.googlesource.gerrit.plugins.github.wizard;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gerrit.server.IdentifiedUser;
import com.googlesource.gerrit.plugins.github.oauth.GitHubLogin;



public interface VelocityController {

  void doAction(IdentifiedUser user, GitHubLogin hubLogin,
      HttpServletRequest req, HttpServletResponse resp, ControllerErrors errors)
      throws ServletException, IOException;

}
