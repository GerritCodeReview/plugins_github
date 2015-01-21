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

import com.google.gerrit.common.data.AccessSection;
import com.google.gerrit.common.data.GroupDescription;
import com.google.gerrit.common.data.GroupReference;
import com.google.gerrit.common.data.Permission;
import com.google.gerrit.common.data.PermissionRule;
import com.google.gerrit.extensions.common.InheritableBoolean;
import com.google.gerrit.extensions.common.SubmitType;
import com.google.gerrit.reviewdb.client.AccountGroup;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.reviewdb.client.Project.NameKey;
import com.google.gerrit.server.account.GroupBackend;
import com.google.gerrit.server.git.MetaDataUpdate;
import com.google.gerrit.server.git.MetaDataUpdate.User;
import com.google.gerrit.server.git.ProjectConfig;
import com.google.gerrit.server.project.ProjectCache;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import com.googlesource.gerrit.plugins.github.GitHubConfig;
import com.googlesource.gerrit.plugins.github.GitHubURL;

import org.eclipse.jgit.lib.ProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateProjectStep extends ImportStep {
  private static final Logger LOG = LoggerFactory.getLogger(CreateProjectStep.class);
  private static final String CODE_REVIEW_REFS = "refs/for/refs/*";
  private static final String TAGS_REFS = "refs/tags/*";
  private static final String CODE_REVIEW_LABEL = "Code-Review";
  private static final String VERIFIED_LABEL = "Verified";
  
  private final String organisation;
  private final String repository;

  private User metaDataUpdateFactory;
  private String description;
  private GroupBackend groupBackend;
  private String username;
  private ProjectConfig projectConfig;
  private ProjectCache projectCache;
  private GitHubConfig config;

  public interface Factory {
    CreateProjectStep create(@Assisted("organisation") String organisation,
        @Assisted("name") String repository,
        @Assisted("description") String description,
        @Assisted("username") String username);
  }

  @Inject
  public CreateProjectStep(@GitHubURL String gitHubUrl,
      MetaDataUpdate.User metaDataUpdateFactory, 
      GroupBackend groupBackend,
      ProjectCache projectCache,
      GitHubRepository.Factory ghRepoFactory,
      GitHubConfig gitHubConfig,
      @Assisted("organisation") String organisation,
      @Assisted("name") String repository,
      @Assisted("description") String description,
      @Assisted("username") String username) {
    super(gitHubUrl, organisation, repository, ghRepoFactory);
    LOG.debug("Gerrit CreateProject " + organisation + "/" + repository);

    this.organisation = organisation;
    this.repository = repository;
    this.description = description;
    this.metaDataUpdateFactory = metaDataUpdateFactory;
    this.groupBackend = groupBackend;
    this.projectCache = projectCache;
    this.username = username;
    this.config = gitHubConfig;
  }
  
  private void setProjectPermissions() {
    addPermissions(AccessSection.ALL, Permission.OWNER);

    addPermissions(CODE_REVIEW_REFS, Permission.READ, Permission.PUSH,
        Permission.REMOVE_REVIEWER, Permission.SUBMIT, Permission.REBASE);

    PermissionRule reviewRange = new PermissionRule(getMyGroup());
    reviewRange.setMin(-2);
    reviewRange.setMax(+2);
    addPermission(CODE_REVIEW_REFS, Permission.LABEL + CODE_REVIEW_LABEL,
        reviewRange);

    PermissionRule verifiedRange = new PermissionRule(getMyGroup());
    verifiedRange.setMin(-1);
    verifiedRange.setMax(+1);
    addPermission(CODE_REVIEW_REFS, Permission.LABEL + VERIFIED_LABEL,
        verifiedRange);

    addPermissions(AccessSection.HEADS, Permission.READ, Permission.CREATE,
        Permission.PUSH_MERGE);

    PermissionRule forcePush = new PermissionRule(getMyGroup());
    forcePush.setForce(true);
    addPermission(AccessSection.HEADS, Permission.PUSH, forcePush);

    addPermissions(TAGS_REFS, Permission.PUSH_TAG, Permission.PUSH_SIGNED_TAG);

    PermissionRule removeTag = new PermissionRule(getMyGroup());
    removeTag.setForce(true);
    addPermission(TAGS_REFS, Permission.PUSH, removeTag);
  }
  
  private void addPermissions(String refSpec, String... permissions) {
    AccessSection accessSection = projectConfig.getAccessSection(refSpec, true);
    for (String permission : permissions) {
      String[] permParts = permission.split("=");
      String action = permParts[0];
      PermissionRule rule;
      if (permParts.length > 1) {
        rule = PermissionRule.fromString(permParts[1], true);
        rule.setGroup(getMyGroup());
      } else {
        rule = new PermissionRule(getMyGroup());
      }
      accessSection.getPermission(action, true).add(rule);
    }
  }

  private void addPermission(String refSpec, String action, PermissionRule rule) {
    projectConfig.getAccessSection(refSpec, true).getPermission(action, true)
        .add(rule);
  }
  
  private GroupReference getMyGroup() {
    GroupDescription.Basic g =
        groupBackend.get(AccountGroup.UUID.parse("user:" + username));
    return projectConfig.resolve(GroupReference.forGroup(g));
  }

  private NameKey getProjectNameKey() {
    return Project.NameKey.parse(organisation + "/" + repository);
  }

  @Override
  public void doImport(ProgressMonitor progress) throws Exception {
    MetaDataUpdate md = null;
    try {
      md = metaDataUpdateFactory.create(getProjectNameKey());
      projectConfig = ProjectConfig.read(md);
      progress.beginTask("Configure Gerrit project", 2);
      setProjectSettings();
      progress.update(1);
      setProjectPermissions();
      progress.update(1);
      md.setMessage("Imported from " + getSourceUri());
      projectConfig.commit(md);
      projectCache.onCreateProject(getProjectNameKey());
    } finally {
      if(md != null) { 
        md.close();
      }
      progress.endTask();
    }
  }
  
  private void setProjectSettings() {
    Project project = projectConfig.getProject();
    project.setParentName(config.getBaseProject(getRepository().isPrivate()));
    project.setDescription(description);
    project.setSubmitType(SubmitType.MERGE_IF_NECESSARY);
    project.setUseContributorAgreements(InheritableBoolean.INHERIT);
    project.setUseSignedOffBy(InheritableBoolean.INHERIT);
    project.setUseContentMerge(InheritableBoolean.INHERIT);
    project.setRequireChangeID(InheritableBoolean.INHERIT);
  }

  @Override
  public boolean rollback() {
    return false;
  }
}
