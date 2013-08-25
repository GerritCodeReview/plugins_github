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

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.common.data.AccessSection;
import com.google.gerrit.common.data.GroupDescription;
import com.google.gerrit.common.data.GroupReference;
import com.google.gerrit.common.data.Permission;
import com.google.gerrit.common.data.PermissionRule;
import com.google.gerrit.reviewdb.client.AccountGroup;
import com.google.gerrit.reviewdb.client.AccountGroup.UUID;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.reviewdb.client.Project.InheritableBoolean;
import com.google.gerrit.reviewdb.client.Project.NameKey;
import com.google.gerrit.reviewdb.client.Project.SubmitType;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.GroupBackend;
import com.google.gerrit.server.git.MetaDataUpdate;
import com.google.gerrit.server.git.MetaDataUpdate.User;
import com.google.gerrit.server.git.ProjectConfig;
import com.google.gerrit.server.project.ProjectCache;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

public class GitClone {
  private static final String GITHUB_REPOSITORY_FORMAT =
      "https://github.com/%1$s/%2$s.git";
  private static final Logger log = LoggerFactory.getLogger(GitCloner.class);

  private static final String CODE_REVIEW_REFS = "refs/for/refs/*";
  private static final String TAGS_REFS = "refs/tags/*";
  private static final String CODE_REVIEW_LABEL = "Code-Review";
  private static final String VERIFIED_LABEL = "Verified";

  private final String organisation;
  private final String repository;

  private final File gitDir;
  private String sourceUri;
  private File destinationDirectory;
  private User metaDataUpdateFactory;
  private String description;
  private GroupBackend groupBackend;
  private String username;
  private ProjectConfig config;

  public interface Factory {
    GitClone create(@Assisted("organisation") String organisation,
        @Assisted("name") String repository,
        @Assisted("description") String description,
        @Assisted("username") String username);
  }

  @Inject
  public GitClone(GitConfig gitConfig,
      MetaDataUpdate.User metaDataUpdateFactory, GroupBackend groupBackend,
      @Assisted("organisation") String organisation,
      @Assisted("name") String repository,
      @Assisted("description") String description,
      @Assisted("username") String username)
      throws GitDestinationAlreadyExistsException,
      GitDestinationNotWritableException {
    this.gitDir = gitConfig.gitDir;
    this.organisation = organisation;
    this.repository = repository;
    this.description = description;
    this.sourceUri = getSourceUri(organisation, repository);
    this.destinationDirectory =
        getDestinationDirectory(organisation, repository);
    this.metaDataUpdateFactory = metaDataUpdateFactory;
    this.groupBackend = groupBackend;
    this.username = username;
  }

  public void doClone(ProgressMonitor progress) throws GitCloneFailedException,
      GitDestinationAlreadyExistsException, GitDestinationNotWritableException {
    CloneCommand clone = new CloneCommand();
    clone.setURI(sourceUri);
    clone.setBare(true);
    clone.setDirectory(destinationDirectory);
    if (progress != null) {
      clone.setProgressMonitor(progress);
    }
    try {
      log.info(sourceUri + "| Clone into " + destinationDirectory);
      clone.call();
    } catch (Throwable e) {
      throw new GitCloneFailedException(sourceUri, e);
    }
  }

  public void configureProject(ProgressMonitor progress)
      throws RepositoryNotFoundException, IOException, ConfigInvalidException {
    MetaDataUpdate md = metaDataUpdateFactory.create(getProjectNameKey());
    try {
      config = ProjectConfig.read(md);
      progress.beginTask("Configure Gerrit project", 2);
      setProjectSettings();
      progress.update(1);
      setProjectPermissions();
      progress.update(1);
      md.setMessage("Imported from " + sourceUri);
      config.commit(md);
    } finally {
      md.close();
      progress.endTask();
    }
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

  private void setProjectSettings() {
    Project project = config.getProject();
    project.setDescription(description);
    project.setSubmitType(SubmitType.MERGE_IF_NECESSARY);
    project.setUseContributorAgreements(InheritableBoolean.INHERIT);
    project.setUseSignedOffBy(InheritableBoolean.INHERIT);
    project.setUseContentMerge(InheritableBoolean.INHERIT);
    project.setRequireChangeID(InheritableBoolean.INHERIT);
  }

  private void addPermissions(String refSpec, String... permissions) {
    AccessSection accessSection = config.getAccessSection(refSpec, true);
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
    config.getAccessSection(refSpec, true).getPermission(action, true)
        .add(rule);
  }


  private GroupReference getMyGroup() {
    GroupDescription.Basic g =
        groupBackend.get(AccountGroup.UUID.parse("user:" + username));
    return config.resolve(GroupReference.forGroup(g));
  }

  private NameKey getProjectNameKey() {
    return Project.NameKey.parse(organisation + "/" + repository);
  }

  private File getDestinationDirectory(String organisation, String repository)
      throws GitDestinationAlreadyExistsException,
      GitDestinationNotWritableException {
    File orgDirectory = new File(gitDir, organisation);
    File destDirectory = new File(orgDirectory, repository + ".git");
    if (destDirectory.exists() && isNotEmpty(destDirectory)) {
      throw new GitDestinationAlreadyExistsException(destDirectory);
    }

    if (!orgDirectory.exists()) {
      if (!orgDirectory.mkdirs()) {
        throw new GitDestinationNotWritableException(destDirectory);
      }
    }

    return destDirectory;
  }

  private boolean isNotEmpty(File destDirectory) {
    return destDirectory.listFiles().length > 0;
  }

  private String getSourceUri(String organisation, String repository) {
    return String.format(GITHUB_REPOSITORY_FORMAT, organisation, repository);
  }

  public String getOrganisation() {
    return organisation;
  }

  public String getRepository() {
    return repository;
  }

  public String getSourceUri() {
    return sourceUri;
  }

  public File getDestinationDirectory() {
    return destinationDirectory;
  }

  public boolean cleanUp() {
    File gitDirectory = destinationDirectory;
    if(!gitDirectory.exists()) {
      return false;
    }
    
    try {
      FileUtils.deleteDirectory(gitDirectory);
      return true;
    } catch (IOException e) {
      log.error("Cannot clean-up output Git directory " + gitDirectory);
      return false;
    }
  }
}
