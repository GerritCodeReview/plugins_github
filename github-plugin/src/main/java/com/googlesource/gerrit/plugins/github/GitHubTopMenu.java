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

import com.google.gerrit.extensions.annotations.Listen;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.webui.TopMenu;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.IdentifiedUser;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Listen
@Singleton
public class GitHubTopMenu implements TopMenu {
  private List<MenuEntry> menuEntries;
  private Provider<CurrentUser> userProvider;

  @Inject
  public GitHubTopMenu(final @PluginName String pluginName,
      final Provider<CurrentUser> userProvider) {
    String baseUrl = "/plugins/" + pluginName;
    this.menuEntries =
        Arrays.asList(new MenuEntry("GitHub", Arrays.asList(
            getItem("Profile", baseUrl + "/static/account.html"),
            getItem("Repositories", baseUrl + "/static/repositories.html"),
            getItem("Pull Requests", baseUrl + "/static/pullrequests.html"))));
    this.userProvider = userProvider;
  }

  private MenuItem getItem(String anchorName, String urlPath) {
    return new MenuItem(anchorName, urlPath, "");
  }

  @Override
  public List<MenuEntry> getEntries() {
    if (userProvider.get() instanceof IdentifiedUser) {
      return menuEntries;
    } else {
      return Collections.emptyList();
    }
  }
}
