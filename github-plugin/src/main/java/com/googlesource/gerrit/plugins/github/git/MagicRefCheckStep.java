// Copyright (C) 2021 The Android Open Source Project
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

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.gerrit.entities.RefNames;
import com.google.gerrit.server.util.MagicBranch;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.googlesource.gerrit.plugins.github.GitHubConfig;
import java.util.List;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.kohsuke.github.GHRef;

public class MagicRefCheckStep extends ImportStep {
  public interface Factory {
    MagicRefCheckStep create(
        @Assisted("organisation") String organisation, @Assisted("name") String repository);
  }

  @Inject
  public MagicRefCheckStep(
      GitHubConfig config,
      GitHubRepository.Factory gitHubRepoFactory,
      @Assisted("organisation") String organisation,
      @Assisted("name") String repository) {
    super(config.gitHubUrl, organisation, repository, gitHubRepoFactory);
  }

  @Override
  public void doImport(ProgressMonitor progress) throws Exception {
    try {
      GHRef[] allRefs = getRepository().getRefs();
      progress.beginTask("Checking magic refs", allRefs.length);

      List<String> offendingRefs = Lists.newLinkedList();
      for (GHRef ref : allRefs) {
        if (MagicBranch.isMagicBranch(ref.getRef())
            || ref.getRef().startsWith(RefNames.REFS_META)) {
          offendingRefs.add(ref.getRef());
        }
        progress.update(1);
      }

      if (!offendingRefs.isEmpty()) {
        throw new MagicRefFoundException(
            String.format(
                "Found %d ref(s): Please remove or rename the following ref(s) and try again: %s",
                offendingRefs.size(), Joiner.on(", ").join(offendingRefs)));
      }
    } finally {
      progress.endTask();
    }
  }

  @Override
  public boolean rollback() {
    return true;
  }
}
