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
package com.googlesrouce.gerrit.plugins.github.git;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.googlesource.gerrit.plugins.github.GitHubURL;

public class GitHubRepository {
  public interface Factory {
    GitHubRepository create(@Assisted("organisation") String organisation,
        @Assisted("repository") String repository);
  }

  public final String cloneUrl;
  public final String organisation;
  public final String repository;

  @Inject
  public GitHubRepository(@GitHubURL String gitHubUrl,
      @Assisted("organisation") String organisation,
      @Assisted("repository") String repository) {
    this.cloneUrl = gitHubUrl + "/" + organisation + "/" + repository + ".git";
    this.organisation = organisation;
    this.repository = repository;
  }
}
