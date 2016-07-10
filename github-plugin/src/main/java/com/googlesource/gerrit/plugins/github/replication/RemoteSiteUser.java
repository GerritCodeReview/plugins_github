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

import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.account.CapabilityControl;
import com.google.gerrit.server.account.GroupMembership;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

public class RemoteSiteUser extends CurrentUser {
  public interface Factory {
    RemoteSiteUser create(@Assisted GroupMembership authGroups);
  }

  private final GroupMembership effectiveGroups;

  @Inject
  RemoteSiteUser(CapabilityControl.Factory capabilityControlFactory,
      @Assisted GroupMembership authGroups) {
    super(capabilityControlFactory);
    effectiveGroups = authGroups;
  }

  @Override
  public GroupMembership getEffectiveGroups() {
    return effectiveGroups;
  }
}
