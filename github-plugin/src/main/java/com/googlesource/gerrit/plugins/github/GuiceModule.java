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

package com.googlesource.gerrit.plugins.github;

import org.apache.velocity.runtime.RuntimeInstance;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import com.googlesource.gerrit.plugins.github.replication.RemoteSiteUser;
import com.googlesource.gerrit.plugins.github.velocity.PluginVelocityRuntimeProvider;
import com.googlesrouce.gerrit.plugins.github.git.CreateProjectStep;
import com.googlesrouce.gerrit.plugins.github.git.GitCloneStep;
import com.googlesrouce.gerrit.plugins.github.git.PullRequestImportJob;
import com.googlesrouce.gerrit.plugins.github.git.PullRequestImporter;
import com.googlesrouce.gerrit.plugins.github.git.ReplicateProjectStep;

public class GuiceModule extends AbstractModule {
  @Override
  protected void configure() {
    install(new FactoryModuleBuilder().implement(GitCloneStep.class,
        GitCloneStep.class).build(GitCloneStep.Factory.class));
    install(new FactoryModuleBuilder().implement(CreateProjectStep.class,
        CreateProjectStep.class).build(CreateProjectStep.Factory.class));
    install(new FactoryModuleBuilder().implement(ReplicateProjectStep.class,
        ReplicateProjectStep.class).build(ReplicateProjectStep.Factory.class));
    install(new FactoryModuleBuilder().implement(PullRequestImportJob.class,
        PullRequestImportJob.class).build(PullRequestImportJob.Factory.class));

    bind(RuntimeInstance.class).annotatedWith(
        Names.named("PluginRuntimeInstance")).toProvider(
        PluginVelocityRuntimeProvider.class);
    
    bind(String.class).annotatedWith(GitHubURL.class).toProvider(GitHubURLProvider.class);
  }
}
