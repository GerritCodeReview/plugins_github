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

import com.google.gerrit.entities.AccessSection;
import com.google.gerrit.entities.AccountGroup;
import com.google.gerrit.entities.BooleanProjectConfig;
import com.google.gerrit.entities.GroupDescription;
import com.google.gerrit.entities.GroupReference;
import com.google.gerrit.entities.Permission;
import com.google.gerrit.entities.PermissionRule;
import com.google.gerrit.entities.Project;
import com.google.gerrit.entities.Project.NameKey;
import com.google.gerrit.extensions.client.InheritableBoolean;
import com.google.gerrit.extensions.client.SubmitType;
import com.google.gerrit.server.account.GroupBackend;
import com.google.gerrit.server.git.meta.MetaDataUpdate;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.project.ProjectConfig;
import com.google.gerrit.server.util.ManualRequestContext;
import com.google.gerrit.server.util.OneOffRequestContext;
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
  private final OneOffRequestContext context;

  private final String organisation;
  private final String repository;
  private final ProjectConfig.Factory projectConfigFactory;

  private MetaDataUpdate.User metaDataUpdateFactory;
  private String description;
  private GroupBackend groupBackend;
  private String username;
  private ProjectConfig projectConfig;
  private ProjectCache projectCache;
  private GitHubConfig config;

  public interface Factory {
    CreateProjectStep create(
        @Assisted("organisation") String organisation,
        @Assisted("name") String repository,
        @Assisted("description") String description,
        @Assisted("username") String username);
  }

  @Inject
  public CreateProjectStep(
      @GitHubURL String gitHubUrl,
      MetaDataUpdate.User metaDataUpdateFactory,
      GroupBackend groupBackend,
      ProjectCache projectCache,
      GitHubRepository.Factory ghRepoFactory,
      GitHubConfig gitHubConfig,
      OneOffRequestContext context,
      ProjectConfig.Factory projectConfigFactory,
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
    this.context = context;
    this.projectConfigFactory = projectConfigFactory;
  }

  private void setProjectPermissions() {
    addPermissions(AccessSection.ALL, Permission.OWNER);

    addPermissions(
        CODE_REVIEW_REFS,
        Permission.READ,
        Permission.PUSH,
        Permission.REMOVE_REVIEWER,
        Permission.SUBMIT,
        Permission.REBASE);

    PermissionRule.Builder reviewRangeBuilder = PermissionRule.create(getMyGroup()).toBuilder();
    reviewRangeBuilder.setMin(-2).setMax(2);
    addPermission(
        CODE_REVIEW_REFS, Permission.LABEL + CODE_REVIEW_LABEL, reviewRangeBuilder.build());

    PermissionRule.Builder verifiedRangeBuilder = PermissionRule.create(getMyGroup()).toBuilder();
    verifiedRangeBuilder.setMin(-1).setMax(1);
    addPermission(
        CODE_REVIEW_REFS, Permission.LABEL + VERIFIED_LABEL, verifiedRangeBuilder.build());

    addPermissions(AccessSection.HEADS, Permission.READ, Permission.CREATE, Permission.PUSH_MERGE);

    PermissionRule.Builder forcePushBuilder = PermissionRule.create(getMyGroup()).toBuilder();
    forcePushBuilder.setForce(true);
    addPermission(AccessSection.HEADS, Permission.PUSH, forcePushBuilder.build());

    addPermissions(TAGS_REFS, Permission.PUSH);

    PermissionRule.Builder removeTagBuilder = PermissionRule.create(getMyGroup()).toBuilder();
    removeTagBuilder.setForce(true);
    addPermission(TAGS_REFS, Permission.PUSH, removeTagBuilder.build());
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
    projectConfig.getAccessSection(refSpec, true).getPermission(action, true).add(rule);
  }

  private GroupReference getMyGroup() {
    GroupDescription.Basic g = groupBackend.get(AccountGroup.UUID.parse("user:" + username));
    return projectConfig.resolve(GroupReference.forGroup(g));
  }

  private NameKey getProjectNameKey() {
    return Project.NameKey.parse(organisation + "/" + repository);
  }

  @Override
  public void doImport(ProgressMonitor progress) throws Exception {
    MetaDataUpdate md = null;
    try (ManualRequestContext requestContext = context.openAs(config.importAccountId)) {
      md = metaDataUpdateFactory.create(getProjectNameKey());
      projectConfig = projectConfigFactory.read(md);
      progress.beginTask("Configure Gerrit project", 2);
      setProjectSettings();
      progress.update(1);
      setProjectPermissions();
      progress.update(1);
      md.setMessage("Imported from " + getSourceUri());
      projectConfig.commit(md);
      projectCache.onCreateProject(getProjectNameKey());
    } finally {
      if (md != null) {
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
    project.setBooleanConfig(
        BooleanProjectConfig.USE_CONTRIBUTOR_AGREEMENTS, InheritableBoolean.INHERIT);
    project.setBooleanConfig(BooleanProjectConfig.USE_SIGNED_OFF_BY, InheritableBoolean.INHERIT);
    project.setBooleanConfig(BooleanProjectConfig.USE_CONTENT_MERGE, InheritableBoolean.INHERIT);
    project.setBooleanConfig(BooleanProjectConfig.REQUIRE_CHANGE_ID, InheritableBoolean.INHERIT);
  }

  @Override
  public boolean rollback() {
    return false;
  }
}
