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

import java.io.IOException;

import org.kohsuke.github.GHEventPayload.PullRequest;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.github.git.PullRequestImportType;
import com.googlesource.gerrit.plugins.github.git.PullRequestImporter;

/**
 * Handles pull_request event in github webhook.
 *
 * @see <a href=
 *      "https://developer.github.com/v3/activity/events/types/#pullrequestevent">
 *      Pull Request Event</a>
 */
@Singleton
class PullRequestHandler implements WebhookEventHandler<PullRequest> {
  private static final Logger logger = LoggerFactory
      .getLogger(PullRequestHandler.class);
  private final Provider<PullRequestImporter> prImportProvider;

  @Inject
  public PullRequestHandler(Provider<PullRequestImporter> pullRequestsImporter) {
    this.prImportProvider = pullRequestsImporter;
  }

  @Override
  public boolean doAction(PullRequest payload) throws IOException {
    String action = payload.getAction();
    if (action.equals("opened") || action.equals("synchronize")) {
      GHRepository repository = payload.getRepository();
      Integer prNumber = new Integer(payload.getNumber());
      PullRequestImporter prImporter = prImportProvider.get();
      String organization = repository.getOwnerName();
      String name = repository.getName();
      logger.info("Importing {}/{}#{}", organization, name, prNumber);
      prImporter.importPullRequest(0, organization, name, prNumber.intValue(),
          PullRequestImportType.Commits);
      logger.info("Imported {}/{}#{}", organization, name, prNumber);
      return true;
    } else {
      return false;
    }
  }

  @Override
  public Class<PullRequest> getPayloadType() {
    return PullRequest.class;
  }
}
