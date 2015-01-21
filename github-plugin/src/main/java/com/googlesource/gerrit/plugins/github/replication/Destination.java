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

package com.googlesource.gerrit.plugins.github.replication;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.gerrit.common.data.GroupReference;
import com.google.gerrit.reviewdb.client.AccountGroup;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.PluginUser;
import com.google.gerrit.server.account.GroupBackend;
import com.google.gerrit.server.account.GroupBackends;
import com.google.gerrit.server.account.ListGroupMembership;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.gerrit.server.project.ProjectControl;
import com.google.gwtorm.server.SchemaFactory;
import com.google.inject.Injector;

import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

public class Destination {
  private final RemoteConfig remote;
  private final ProjectControl.Factory projectControlFactory;
  private final String remoteNameStyle;
  private final CurrentUser remoteUser;

  Destination(final Injector injector, final RemoteConfig rc, final Config cfg,
      final SchemaFactory<ReviewDb> db,
      final RemoteSiteUser.Factory replicationUserFactory,
      final PluginUser pluginUser,
      final GitRepositoryManager gitRepositoryManager,
      final GroupBackend groupBackend) {
    remote = rc;

    remoteNameStyle =
        MoreObjects.firstNonNull(
            cfg.getString("remote", rc.getName(), "remoteNameStyle"), "slash");

    String[] authGroupNames =
        cfg.getStringList("remote", rc.getName(), "authGroup");
    if (authGroupNames.length > 0) {
      ImmutableSet.Builder<AccountGroup.UUID> builder = ImmutableSet.builder();
      for (String name : authGroupNames) {
        GroupReference g =
            GroupBackends.findExactSuggestion(groupBackend, name);
        if (g != null) {
          builder.add(g.getUUID());
        } else {
          GitHubDestinations.log.warn(String.format(
              "Group \"%s\" not recognized, removing from authGroup", name));
        }
      }
      remoteUser =
          replicationUserFactory
              .create(new ListGroupMembership(builder.build()));
    } else {
      remoteUser = pluginUser;
    }

    projectControlFactory = injector.getInstance(ProjectControl.Factory.class);
  }

  ProjectControl controlFor(Project.NameKey project)
      throws NoSuchProjectException {
    return projectControlFactory.controlFor(project);
  }

  List<URIish> getURIs(Project.NameKey project, String urlMatch) {
    List<URIish> r = Lists.newArrayListWithCapacity(remote.getURIs().size());
    for (URIish uri : remote.getURIs()) {
      if (matches(uri, urlMatch)) {
        String name = project.get();
        if (needsUrlEncoding(uri)) {
          name = encode(name);
        }
        if (remoteNameStyle.equals("dash")) {
          name = name.replace("/", "-");
        } else if (remoteNameStyle.equals("underscore")) {
          name = name.replace("/", "_");
        } else if (!remoteNameStyle.equals("slash")) {
          GitHubDestinations.log.debug(String.format(
              "Unknown remoteNameStyle: %s, falling back to slash",
              remoteNameStyle));
        }
        String replacedPath =
            GitHubDestinations.replaceName(uri.getPath(), name);
        if (replacedPath != null) {
          uri = uri.setPath(replacedPath);
          r.add(uri);
        }
      }
    }
    return r;
  }

  static boolean needsUrlEncoding(URIish uri) {
    return "http".equalsIgnoreCase(uri.getScheme())
        || "https".equalsIgnoreCase(uri.getScheme())
        || "amazon-s3".equalsIgnoreCase(uri.getScheme());
  }

  static String encode(String str) {
    try {
      // Some cleanup is required. The '/' character is always encoded as %2F
      // however remote servers will expect it to be not encoded as part of the
      // path used to the repository. Space is incorrectly encoded as '+' for
      // this
      // context. In the path part of a URI space should be %20, but in form
      // data
      // space is '+'. Our cleanup replace fixes these two issues.
      return URLEncoder.encode(str, "UTF-8").replaceAll("%2[fF]", "/")
          .replace("+", "%20");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  private static boolean matches(URIish uri, String urlMatch) {
    if (urlMatch == null || urlMatch.equals("") || urlMatch.equals("*")) {
      return true;
    }
    return uri.toString().contains(urlMatch);
  }

  public RemoteConfig getRemote() {
    return remote;
  }

  public CurrentUser getRemoteUser() {
    return remoteUser;
  }
}
