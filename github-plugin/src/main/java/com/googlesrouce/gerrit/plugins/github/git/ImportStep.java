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

import org.eclipse.jgit.lib.ProgressMonitor;



public abstract class ImportStep {
  protected static final String GITHUB_REPOSITORY_BASE_URI =
      "https://github.com";
  private static final String GITHUB_REPOSITORY_FORMAT =
      GITHUB_REPOSITORY_BASE_URI + "/%1$s/%2$s.git";
  private String organisation;
  private String repository;

  public ImportStep(String organisation, String repository) {
    this.organisation = organisation;
    this.repository = repository;
  }

  protected String getSourceUri() {
    return String.format(GITHUB_REPOSITORY_FORMAT, organisation, repository);
  }
  
  public String getOrganisation() {
    return organisation;
  }

  public String getRepository() {
    return repository;
  }

  public abstract void doImport(ProgressMonitor progress) throws Exception;

  public abstract boolean rollback();
}
