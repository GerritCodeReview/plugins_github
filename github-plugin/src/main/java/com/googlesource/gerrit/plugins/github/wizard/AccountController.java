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

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gerrit.extensions.restapi.RawInput;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.reviewdb.client.Account.Id;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.AccountCache;
import com.google.gerrit.server.account.AccountException;
import com.google.gerrit.server.account.AccountManager;
import com.google.gerrit.server.account.AccountResource;
import com.google.gerrit.server.account.AddSshKey;
import com.google.gerrit.server.account.AuthRequest;
import com.google.gerrit.server.account.AuthResult;
import com.google.gerrit.server.account.GetSshKeys;
import com.google.gerrit.server.account.GetSshKeys.SshKeyInfo;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.google.inject.Provider;

import com.googlesource.gerrit.plugins.github.oauth.GitHubLogin;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.github.GHKey;
import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GHVerifiedKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AccountController implements VelocityController {

  private static final Logger log = LoggerFactory
      .getLogger(AccountController.class);
  private final AddSshKey restAddSshKey;
  private final GetSshKeys restGetSshKeys;
  private final AccountManager accountManager;
  private final Provider<ReviewDb> dbProvider;
  private final AccountCache accountCache;

  @Inject
  public AccountController(final AddSshKey restAddSshKey,
      final GetSshKeys restGetSshKeys, final AccountManager accountManager,
      final Provider<ReviewDb> dbProvider, final AccountCache accountCache) {
    this.restAddSshKey = restAddSshKey;
    this.restGetSshKeys = restGetSshKeys;
    this.accountManager = accountManager;
    this.dbProvider = dbProvider;
    this.accountCache = accountCache;
  }

  public void doAction(IdentifiedUser user, GitHubLogin hubLogin,
      HttpServletRequest req, HttpServletResponse resp, ControllerErrors errors)
      throws ServletException, IOException {
    try {
      setAccountIdentity(user, req);
      setAccoutPublicKeys(user, hubLogin, req);

      log.info("Created account '" + user.getUserName() + "'");
    } catch (IOException e) {
      log.error("Account '" + user.getUserName() + "' creation failed", e);
      throw e;
    }
  }

  private void setAccountIdentity(IdentifiedUser user, HttpServletRequest req) throws ServletException {
    String fullName = req.getParameter("fullname");
    String email = req.getParameter("email");
    try {
      Id accountId = user.getAccountId();
      AuthResult result =
          accountManager.link(accountId, AuthRequest.forEmail(email));
      log.debug("Account {} linked to email {}: result = {}", accountId, email,
          result);

      Account a = dbProvider.get().accounts().get(accountId);
      a.setPreferredEmail(email);
      a.setFullName(fullName);
      dbProvider.get().accounts().update(Collections.singleton(a));
      log.debug(
          "Account {} updated with preferredEmail = {} and fullName = {}",
          accountId, email, fullName);

      accountCache.evict(accountId);
      log.debug("Account cache evicted for {}", accountId);
    } catch (AccountException | OrmException e) {
      throw new ServletException("Cannot associate email '" + email
          + "' to current user '" + user + "'", e);
    }
  }

  private void setAccoutPublicKeys(IdentifiedUser user, GitHubLogin hubLogin,
      HttpServletRequest req) throws IOException {
    GHMyself myself = hubLogin.getMyself();
    List<GHVerifiedKey> githubKeys = myself.getPublicVerifiedKeys();
    HashSet<String> gerritKeys = Sets.newHashSet(getCurrentGerritSshKeys(user));
    for (GHKey ghKey : githubKeys) {
      String sshKeyCheckedParam = "key_check_" + ghKey.getId();
      String sshKeyWithLabel = ghKey.getKey() + " " + ghKey.getTitle();
      String checked = req.getParameter(sshKeyCheckedParam);
      if (checked != null && checked.equalsIgnoreCase("on")
          && !gerritKeys.contains(ghKey.getKey())) {
        addSshKey(user, sshKeyWithLabel);
        gerritKeys.add(ghKey.getKey());
      }
    }
  }

  private List<String> getCurrentGerritSshKeys(final IdentifiedUser user)
      throws IOException {
    AccountResource res = new AccountResource(user);
    try {
      List<SshKeyInfo> keysInfo = restGetSshKeys.apply(res);
      return Lists.transform(keysInfo, new Function<SshKeyInfo, String>() {

        @Override
        public String apply(SshKeyInfo keyInfo) {
          return StringUtils.substringBeforeLast(keyInfo.sshPublicKey, " ");
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
