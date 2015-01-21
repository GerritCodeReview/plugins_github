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

package com.googlesource.gerrit.plugins.github.git;

import com.google.common.base.Optional;
import com.google.common.base.Strings;

import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitUser;

import java.io.IOException;

import lombok.Getter;

public class GitHubUser {
  @Getter
  private final String login;
  @Getter
  private final String name;
  @Getter
  private final String email;

  private GitHubUser(GHUser gitHubUser, GitUser author) throws IOException {
    this.login = initLogin(gitHubUser).or(generateLogin(author.getName()));
    this.name = initFullName(gitHubUser).or(author.getName());
    this.email = initEmail(gitHubUser).or(author.getEmail());
  }

  private static String generateLogin(String fullName) {
    return fullName.toLowerCase().replaceAll("^[a-z0-9]", "_");
  }

  private static Optional<String> initLogin(GHUser gitHubUser) {
    return Optional.fromNullable(gitHubUser != null ? gitHubUser.getLogin()
        : null);
  }

  private static Optional<String> initEmail(GHUser gitHubUser)
      throws IOException {
    return Optional.fromNullable(gitHubUser != null ? Strings
        .emptyToNull(gitHubUser.getEmail()) : null);
  }

  private static Optional<String> initFullName(GHUser gitHubUser)
      throws IOException {
    return Optional.fromNullable(gitHubUser != null ? Strings
        .emptyToNull(gitHubUser.getName()) : null);
  }

  public static GitHubUser from(GHUser gitHubUser, GitUser author)
      throws IOException {
    return new GitHubUser(gitHubUser, author);
  }
}
