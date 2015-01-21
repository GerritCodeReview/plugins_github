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
import com.google.inject.Provider;
import com.google.inject.Singleton;

import com.googlesource.gerrit.plugins.github.git.PullRequestImportType;
import com.googlesource.gerrit.plugins.github.git.PullRequestImporter;
import com.googlesource.gerrit.plugins.github.oauth.GitHubLogin;

import java.io.IOException;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Singleton
public class PullRequestImportController implements VelocityController {

  private Provider<PullRequestImporter> prImportProvider;

  @Inject
  public PullRequestImportController(
      final Provider<PullRequestImporter> pullRequestsImporter) {
    this.prImportProvider = pullRequestsImporter;
  }

  @Override
  public void doAction(IdentifiedUser user, GitHubLogin hubLogin,
      HttpServletRequest req, HttpServletResponse resp, ControllerErrors errors)
      throws ServletException, IOException {
    String organisation = req.getParameter("organisation");
    PullRequestImporter prImporter = prImportProvider.get();

    for (Entry<String, String[]> param : req.getParameterMap().entrySet()) {
      String name = param.getKey();
      if (name.endsWith(".selected") && param.getValue().length == 1
          && param.getValue()[0].equalsIgnoreCase("on")) {

        String paramPrefix =
            name.substring(0, name.length() - ".selected".length());
        int idx = Integer.parseInt(req.getParameter(paramPrefix + ".idx"));
        PullRequestImportType importType =
            PullRequestImportType.valueOf(req.getParameter(paramPrefix
                + ".type"));
        
        int pullRequestId =
            Integer.parseInt(req.getParameter(paramPrefix + ".id"));
        String repoName =
           req.getParameter(paramPrefix + ".repo");

        prImporter.importPullRequest(idx, organisation, repoName,
            pullRequestId, importType);
      }
    }

  }
}
