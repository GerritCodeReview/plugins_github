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
import com.google.gerrit.server.PluginUser;
import com.google.gerrit.server.account.GroupBackend;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.replication.MergedConfigResource;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private final List<Destination> configs;

  private final RemoteSiteUser.Factory replicationUserFactory;
  private final PluginUser pluginUser;
  private final GroupBackend groupBackend;
  private final List<String> organisations;

  @Inject
  GitHubDestinations(
      final MergedConfigResource replicationConfig,
      final RemoteSiteUser.Factory ruf,
      final GroupBackend gb,
      final PluginUser pu)
      throws ConfigInvalidException {
    pluginUser = pu;
    replicationUserFactory = ruf;
    groupBackend = gb;
    configs = getDestinations(replicationConfig.getConfig());
    organisations = getOrganisations(configs);
  }

  private List<String> getOrganisations(List<Destination> destinations) {
    ArrayList<String> result = new ArrayList<>();
    for (Destination destination : destinations) {
      for (URIish urish : destination.getRemote().getURIs()) {
        String[] uriPathParts = urish.getPath().split("/");
        result.add(uriPathParts[0]);
      }
    }
    return result;
  }

  private List<Destination> getDestinations(Config cfg) throws ConfigInvalidException {
    ImmutableList.Builder<Destination> dest = ImmutableList.builder();
    for (RemoteConfig c : allRemotes(cfg)) {
      if (c.getURIs().isEmpty()) {
        continue;
      }

      for (URIish u : c.getURIs()) {
        if (u.getPath() == null || !u.getPath().contains("${name}")) {
          throw new ConfigInvalidException(
              String.format("remote.%s.url \"%s\" lacks ${name} placeholder", c.getName(), u));
        }
      }

      // If destination for push is not set assume equal to source.
      for (RefSpec ref : c.getPushRefSpecs()) {
        if (ref.getDestination() == null) {
          ref.setDestination(ref.getSource());
        }
      }

      if (c.getPushRefSpecs().isEmpty()) {
        c.addPushRefSpec(
            new RefSpec().setSourceDestination("refs/*", "refs/*").setForceUpdate(true));
      }

      dest.add(new Destination(c, cfg, replicationUserFactory, pluginUser, groupBackend));
    }
    return dest.build();
  }

  private static List<RemoteConfig> allRemotes(Config cfg) throws ConfigInvalidException {
    Set<String> names = cfg.getSubsections("remote");
    List<RemoteConfig> result = Lists.newArrayListWithCapacity(names.size());
    for (String name : names) {
      try {
        if (name.equalsIgnoreCase(GITHUB_DESTINATION)) {
          result.add(new RemoteConfig(cfg, name));
        }
      } catch (URISyntaxException e) {
        throw new ConfigInvalidException(String.format("remote %s has invalid URL", name));
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
