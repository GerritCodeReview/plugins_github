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
package com.googlesource.gerrit.plugins.github.oauth;

import com.google.common.base.MoreObjects;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class GitHubLogoutServletResponse extends HttpServletResponseWrapper {
  private String redirectUrl;

  public GitHubLogoutServletResponse(HttpServletResponse response, String redirectUrl) {
    super(response);
    this.redirectUrl = redirectUrl;
  }

  @Override
  public void sendRedirect(String location) throws IOException {
    super.sendRedirect(MoreObjects.firstNonNull(redirectUrl, location));
  }
}
