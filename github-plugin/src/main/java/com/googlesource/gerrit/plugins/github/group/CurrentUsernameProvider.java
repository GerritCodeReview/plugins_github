// Copyright (C) 2023 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.github.group;

import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.IdentifiedUser;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.Optional;

public class CurrentUsernameProvider implements Provider<String> {
  public static final String CURRENT_USERNAME = "CurrentUsername";

  private final Provider<CurrentUser> userProvider;

  @Inject
  CurrentUsernameProvider(Provider<CurrentUser> userProvider) {
    this.userProvider = userProvider;
  }

  @Override
  public String get() {
    return Optional.ofNullable(userProvider.get())
        .filter(CurrentUser::isIdentifiedUser)
        .map(CurrentUser::asIdentifiedUser)
        .flatMap(IdentifiedUser::getUserName)
        .orElse(null);
  }
}
