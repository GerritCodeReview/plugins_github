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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.PluginUser;
import com.google.gerrit.server.account.GroupBackend;
import com.google.gerrit.server.config.SitePaths;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gwtorm.server.SchemaFactory;
import com.google.inject.Inject;
import com.google.inject.Injector;

import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/** Manages automatic replication to remote repositories. */
public class GitHubDestinations {
  private static final String GITHUB_DESTINATION = "github";
  static final Logger log = LoggerFactory.getLogger(GitHubDestinations.class);

  static String replaceName(String in, String name) {
    String key = "${name}";
    int n = in.indexOf(key);
    if (0 <= n) {
      return in.substring(0, n) + name + in.substring(n + key.length());
    }
    return null;
  }

  private final Injector injector;
  private final List<Destination> configs;


  private final SchemaFactory<ReviewDb> database;
  private final RemoteSiteUser.Factory replicationUserFactory;
  private final PluginUser pluginUser;
  private final GitRepositoryManager gitRepositoryManager;
  private final GroupBackend groupBackend;
  boolean replicateAllOnPluginStart;
  private final List<String> organisations;

  @Inject
  GitHubDestinations(final Injector i, final SitePaths site,
      final RemoteSiteUser.Factory ruf, final SchemaFactory<ReviewDb> db,
      final GitRepositoryManager grm, final GroupBackend gb, final PluginUser pu)
      throws ConfigInvalidException, IOException {
    injector = i;
    database = db;
    pluginUser = pu;
    replicationUserFactory = ruf;
    gitRepositoryManager = grm;
    groupBackend = gb;
    configs = getDestinations(new File(site.etc_dir, "replication.config"));
    organisations = getOrganisations(configs);
  }

  private List<String> getOrganisations(List<Destination> destinations) {
    ArrayList<String> organisations = new ArrayList<String>();
    for (Destination destination : destinations) {
      for (URIish urish : destination.getRemote().getURIs()) {
        String[] uriPathParts = urish.getPath().split("/");
        organisations.add(uriPathParts[0]);
      }
    }
    return organisations;
  }

  private List<Destination> getDestinations(File cfgPath)
      throws ConfigInvalidException, IOException {
    FileBasedConfig cfg = new FileBasedConfig(cfgPath, FS.DETECTED);
    if (!cfg.getFile().exists() || cfg.getFile().length() == 0) {
      return Collections.emptyList();
    }

    try {
      cfg.load();
    } catch (ConfigInvalidException e) {
      throw new ConfigInvalidException(String.format(
          "Config file %s is invalid: %s", cfg.getFile(), e.getMessage()), e);
    } catch (IOException e) {
      throw new IOException(String.format("Cannot read %s: %s", cfg.getFile(),
          e.getMessage()), e);
    }

    ImmutableList.Builder<Destination> dest = ImmutableList.builder();
    for (RemoteConfig c : allRemotes(cfg)) {
      if (c.getURIs().isEmpty()) {
        continue;
      }

      for (URIish u : c.getURIs()) {
        if (u.getPath() == null || !u.getPath().contains("${name}")) {
          throw new ConfigInvalidException(String.format(
              "remote.%s.url \"%s\" lacks ${name} placeholder in %s",
              c.getName(), u, cfg.getFile()));
        }
      }

      // If destination for push is not set assume equal to source.
      for (RefSpec ref : c.getPushRefSpecs()) {
        if (ref.getDestination() == null) {
          ref.setDestination(ref.getSource());
        }
      }

      if (c.getPushRefSpecs().isEmpty()) {
        c.addPushRefSpec(new RefSpec().setSourceDestination("refs/*", "refs/*")
            .setForceUpdate(true));
      }

      dest.add(new Destination(injector, c, cfg, database,
          replicationUserFactory, pluginUser, gitRepositoryManager,
          groupBackend));
    }
    return dest.build();
  }

  private static List<RemoteConfig> allRemotes(FileBasedConfig cfg)
      throws ConfigInvalidException {
    Set<String> names = cfg.getSubsections("remote");
    List<RemoteConfig> result = Lists.newArrayListWithCapacity(names.size());
    for (String name : names) {
      try {
        if (name.equalsIgnoreCase(GITHUB_DESTINATION)) {
          result.add(new RemoteConfig(cfg, name));
        }
      } catch (URISyntaxException e) {
        throw new ConfigInvalidException(String.format(
            "remote %s has invalid URL in %s", name, cfg.getFile()));
      }
    }
    return result;
  }

  public List<Destination> getDestinations() {
    return configs;
  }

  public List<String> getOrganisations() {
    return organisations;
  }
}
