// Copyright (C) 2014 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.github.oauth;

import java.io.IOException;

import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

public class GitHubAnonymousLogin extends GitHubLogin {
  private static final Logger LOG = LoggerFactory
      .getLogger(GitHubAnonymousLogin.class);

  @Inject
  public GitHubAnonymousLogin() {
    super(null, null);
  }

  @Override
  public boolean isLoggedIn() {
    return hub != null;
  }

  public synchronized boolean loginAnonymously() {
    if (isLoggedIn()) {
      return true;
    }

    LOG.debug("Login anonymously " + this);
    try {
      this.hub = GitHub.connectAnonymously();
    } catch (IOException e) {
      LOG.error("Cannot login to GitHub", e);
      return false;
    }
    return true;
  }
}
