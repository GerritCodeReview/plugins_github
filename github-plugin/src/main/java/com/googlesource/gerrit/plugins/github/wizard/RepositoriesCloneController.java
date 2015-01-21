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

import com.googlesource.gerrit.plugins.github.git.GitImporter;
import com.googlesource.gerrit.plugins.github.oauth.GitHubLogin;
import com.googlesource.gerrit.plugins.github.oauth.ScopedProvider;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RepositoriesCloneController implements VelocityController {
  private static final String REPO_PARAM_PREFIX = "repo_";
  private final ScopedProvider<GitImporter> cloneProvider;

  @Inject
  public RepositoriesCloneController(ScopedProvider<GitImporter> cloneProvider) {
    this.cloneProvider = cloneProvider;
  }

  @Override
  public void doAction(IdentifiedUser user, GitHubLogin hubLogin,
      HttpServletRequest req, HttpServletResponse resp,
      ControllerErrors errorMgr) throws ServletException, IOException {
    
    GitImporter gitCloner = cloneProvider.get(req);
    gitCloner.reset();
    Set<Entry<String, String[]>> params = req.getParameterMap().entrySet();
    for (Entry<String, String[]> param : params) {
      String paramName = param.getKey();
      String[] paramValue = param.getValue();

      if (!paramName.startsWith(REPO_PARAM_PREFIX) || paramValue.length != 1
          || paramName.split("_").length != 2) {
        continue;
      }
      String repoIdxString = paramName.split("_")[1];
      if(!Character.isDigit(repoIdxString.charAt(0))) {
        continue;
      }
      
      int repoIdx = Integer.parseInt(repoIdxString);
      String organisation =
          req.getParameter(REPO_PARAM_PREFIX + repoIdx + "_organisation");
      String repository =
          req.getParameter(REPO_PARAM_PREFIX + repoIdx + "_repository");
      String description =
          req.getParameter(REPO_PARAM_PREFIX + repoIdx + "_description");
      try {
        gitCloner.clone(repoIdx, organisation, repository,
            description);
      } catch (Exception e) {
        errorMgr.submit(e);
      }

    }
  }
}
