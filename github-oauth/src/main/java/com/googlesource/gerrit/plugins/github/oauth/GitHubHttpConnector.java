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

package com.googlesource.gerrit.plugins.github.oauth;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.kohsuke.github.HttpConnector;

import com.google.inject.Inject;

public class GitHubHttpConnector implements HttpConnector {

  private final GitHubOAuthConfig config;

  @Inject
  public GitHubHttpConnector(final GitHubOAuthConfig config) {
    this.config = config;
  }

  @Override
  public HttpURLConnection connect(URL url) throws IOException {
    HttpURLConnection huc = (HttpURLConnection) url.openConnection();
    HttpURLConnection.setFollowRedirects(true);
    huc.setConnectTimeout((int) config.httpConnectionTimeout);
    huc.setReadTimeout((int) config.httpReadTimeout);
    return huc;
  }

}
