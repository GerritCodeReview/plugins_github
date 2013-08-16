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
package com.googlesource.gerrit.plugins.github.wizard;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;

import javax.annotation.Nullable;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.kohsuke.github.GHKey;
import org.kohsuke.github.GHMyself;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gerrit.extensions.restapi.RawInput;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.AccountResource;
import com.google.gerrit.server.account.AddSshKey;
import com.google.gerrit.server.account.GetSshKeys;
import com.google.gerrit.server.account.GetSshKeys.SshKeyInfo;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.github.oauth.GitHubLogin;

public class AccountController implements VelocityController {

  private static final Logger log = LoggerFactory
      .getLogger(VelocityControllerServlet.class);
  private final AddSshKey restAddSshKey;
  private final GetSshKeys restGetSshKeys;

  @Inject
  public AccountController(final AddSshKey restAddSshKey,
      final GetSshKeys restGetSshKeys) {
    this.restAddSshKey = restAddSshKey;
    this.restGetSshKeys = restGetSshKeys;
  }

  public void doAction(IdentifiedUser user, GitHubLogin hubLogin,
      HttpServletRequest req, HttpServletResponse resp, ControllerErrors errors)
      throws ServletException, IOException {
    GHMyself myself = hubLogin.getMyself();
    List<GHKey> githubKeys = myself.getPublicKeys();
    HashSet<String> gerritKeys = Sets.newHashSet(getSshKeys(user));
    for (GHKey ghKey : githubKeys) {
      String sshKeyCheckedParam = "key_check_" + ghKey.getId();
      String sshKeyWithLabel = ghKey.getKey() + " " + ghKey.getTitle();
      String checked = req.getParameter(sshKeyCheckedParam);
      if (checked != null && checked.equalsIgnoreCase("on")
          && !gerritKeys.contains(sshKeyWithLabel)) {
        addSshKey(user, sshKeyWithLabel);
      }
    }
  }

  private List<String> getSshKeys(final IdentifiedUser user) throws IOException {
    AccountResource res = new AccountResource(user);
    try {
      List<SshKeyInfo> keysInfo = restGetSshKeys.apply(res);
      return Lists.transform(keysInfo, new Function<SshKeyInfo, String>() {

        @Override
        @Nullable
        public String apply(@Nullable SshKeyInfo keyInfo) {
          return keyInfo.sshPublicKey;
        }

      });
    } catch (Exception e) {
      log.error("User list keys failed", e);
      throw new IOException("Cannot get list of user keys", e);
    }
  }

  private void addSshKey(final IdentifiedUser user, final String sshKeyWithLabel)
      throws IOException {
    AccountResource res = new AccountResource(user);
    final ByteArrayInputStream keyIs =
        new ByteArrayInputStream(sshKeyWithLabel.getBytes());
    AddSshKey.Input key = new AddSshKey.Input();
    key.raw = new RawInput() {

      @Override
      public InputStream getInputStream() throws IOException {
        return keyIs;
      }

      @Override
      public String getContentType() {
        return "text/plain";
      }

      @Override
      public long getContentLength() {
        return sshKeyWithLabel.length();
      }
    };
    try {
      restAddSshKey.apply(res, key);
    } catch (Exception e) {
      log.error("Add key " + sshKeyWithLabel + " failed", e);
      throw new IOException("Cannot store SSH Key '" + sshKeyWithLabel + "'", e);
    }
  }
}
