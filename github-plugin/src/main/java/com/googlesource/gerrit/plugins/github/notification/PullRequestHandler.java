// Copyright (C) 2015 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.github.notification;

import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.github.git.PullRequestImportType;
import com.googlesource.gerrit.plugins.github.git.PullRequestImporter;

import org.kohsuke.github.GHEventPayload.PullRequest;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Handles pull_request event in github webhook.
 * 
 * @see <a href=
 *      "https://developer.github.com/v3/activity/events/types/#pullrequestevent">
 *      Pull Request Event</a>
 */
@Singleton
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
