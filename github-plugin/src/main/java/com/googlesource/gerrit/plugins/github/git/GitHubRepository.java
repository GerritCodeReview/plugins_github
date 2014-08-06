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
package com.googlesource.gerrit.plugins.github.git;

import java.io.IOException;

import lombok.experimental.Delegate;

import org.kohsuke.github.GHRepository;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.googlesource.gerrit.plugins.github.GitHubURL;
import com.googlesource.gerrit.plugins.github.oauth.GitHubLogin;
import com.googlesource.gerrit.plugins.github.oauth.ScopedProvider;

public class GitHubRepository extends GHRepository {
  public interface Factory {
    GitHubRepository create(@Assisted("organisation") String organisation,
        @Assisted("repository") String repository);
  }


  private final String organisation;
  private final String repository;
  private final GitHubLogin ghLogin;
  private final String cloneUrl;

  @Delegate
  private GHRepository ghRepository;

  public String getCloneUrl() {
    return cloneUrl.replace("://", "://" + ghLogin.getMyself().getLogin() + ":"
        + ghLogin.getToken().accessToken + "@");
  }

  public String getOrganisation() {
    return organisation;
  }

  public String getRepository() {
    return repository;
  }

  @Inject
  public GitHubRepository(ScopedProvider<GitHubLogin> ghLoginProvider,
      @GitHubURL String gitHubUrl,
      @Assisted("organisation") String organisation,
      @Assisted("repository") String repository) throws IOException {
    this.cloneUrl = gitHubUrl + "/" + organisation + "/" + repository + ".git";
    this.organisation = organisation;
    this.repository = repository;
    this.ghLogin = ghLoginProvider.get();
    this.ghRepository =
        ghLogin.getHub().getRepository(organisation + "/" + repository);
  }
}
