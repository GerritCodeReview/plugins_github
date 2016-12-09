// Copyright (C) 2016 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.github.groups;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.google.common.base.MoreObjects;

public class OrganizationStructure implements Serializable {
  private static final long serialVersionUID = 1L;

  private HashMap<String, HashSet<String>> teams = new HashMap<>();

  public Set<String> put(String organisation, String team) {
    HashSet<String> userTeams =
        MoreObjects.firstNonNull(teams.get(organisation),
            new HashSet<String>());
    userTeams.add(team);
    return teams.put(organisation, userTeams);
  }

  public Set<String> keySet() {
    return teams.keySet();
  }

  public Iterable<String> get(String organization) {
    return teams.get(organization);
  }
}