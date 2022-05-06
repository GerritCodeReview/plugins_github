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

import static com.google.gerrit.server.account.externalids.ExternalId.SCHEME_USERNAME;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gerrit.entities.Account.Id;
import com.google.gerrit.extensions.api.accounts.SshKeyInput;
import com.google.gerrit.extensions.common.NameInput;
import com.google.gerrit.extensions.common.SshKeyInfo;
import com.google.gerrit.extensions.restapi.RawInput;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.ServerInitiated;
import com.google.gerrit.server.account.AccountCache;
import com.google.gerrit.server.account.AccountManager;
import com.google.gerrit.server.account.AccountResource;
import com.google.gerrit.server.account.AccountsUpdate;
import com.google.gerrit.server.account.AuthRequest;
import com.google.gerrit.server.account.AuthResult;
import com.google.gerrit.server.account.externalids.ExternalId;
import com.google.gerrit.server.account.externalids.ExternalIdFactory;
import com.google.gerrit.server.account.externalids.ExternalIds;
import com.google.gerrit.server.config.AuthConfig;
import com.google.gerrit.server.restapi.account.AddSshKey;
import com.google.gerrit.server.restapi.account.GetSshKeys;
import com.google.gerrit.server.restapi.account.PutName;
import com.google.gerrit.server.restapi.account.PutPreferred;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.googlesource.gerrit.plugins.github.oauth.GitHubLogin;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.kohsuke.github.GHKey;
import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GHVerifiedKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccountController implements VelocityController {

  private static final Logger log = LoggerFactory.getLogger(AccountController.class);
  private final AddSshKey restAddSshKey;
  private final GetSshKeys restGetSshKeys;
  private final AccountManager accountManager;
  private final AccountCache accountCache;
  private final PutPreferred putPreferred;
  private final PutName putName;
  private final Provider<AccountsUpdate> accountsUpdateProvider;
  private final ExternalIds externalIds;
  private final ExternalIdFactory externalIdFactory;
  private final AuthRequest.Factory authRequestFactory;
  private final AuthConfig authConfig;

  @Inject
  public AccountController(
      final AddSshKey restAddSshKey,
      final GetSshKeys restGetSshKeys,
      final AccountManager accountManager,
      final AccountCache accountCache,
      final PutPreferred putPreferred,
      final PutName putName,
      @ServerInitiated final Provider<AccountsUpdate> accountsUpdateProvider,
      final ExternalIds externalIds,
      final ExternalIdFactory externalIdFactory,
      final AuthRequest.Factory authRequestFactory,
      final AuthConfig authConfig) {
    this.restAddSshKey = restAddSshKey;
    this.restGetSshKeys = restGetSshKeys;
    this.accountManager = accountManager;
    this.accountCache = accountCache;
    this.putPreferred = putPreferred;
    this.putName = putName;
    this.accountsUpdateProvider = accountsUpdateProvider;
    this.externalIds = externalIds;
    this.externalIdFactory = externalIdFactory;
    this.authRequestFactory = authRequestFactory;
    this.authConfig = authConfig;
  }

  @Override
  public void doAction(
      IdentifiedUser user,
      GitHubLogin hubLogin,
      HttpServletRequest req,
      HttpServletResponse resp,
      ControllerErrors errors)
      throws ServletException, IOException {
    try {
      setAccountIdentity(user, req);
      setAccoutPublicKeys(user, hubLogin, req);

      log.info(
          "Updated account '"
              + user.getAccountId()
              + "' with username='"
              + req.getParameter("username")
              + "'");
    } catch (IOException | ConfigInvalidException e) {
      log.error("Account '" + user.getUserName() + "' creation failed", e);
      throw new IOException(e);
    }
  }

  private void setAccountIdentity(IdentifiedUser user, HttpServletRequest req)
      throws ServletException, ConfigInvalidException {
    String fullName = req.getParameter("fullname");
    String email = req.getParameter("email");
    String username = req.getParameter("username");
    try {
      Id accountId = user.getAccountId();
      AuthResult result = accountManager.link(accountId, authRequestFactory.createForEmail(email));
      log.debug("Account {} linked to email {}: result = {}", accountId, email, result);

      putPreferred.apply(new AccountResource.Email(user, email), null);
      NameInput nameInput = new NameInput();
      nameInput.name = fullName;
      putName.apply(user, nameInput);

      ExternalId.Key key =
          ExternalId.Key.create(SCHEME_USERNAME, username, authConfig.isUserNameCaseInsensitive());
      Optional<ExternalId> other;
      try {
        other = externalIds.get(key);
      } catch (IOException e) {
        throw new IllegalArgumentException(
            "Internal error while fetching username='" + username + "'");
      }

      if (other.map(externalId -> externalId.accountId().equals(accountId)).orElse(false)) {
        // Current account has already an external id with SCHEME_USERNAME set to the username
        return;
      }

      try {
        accountsUpdateProvider
            .get()
            .update(
                "Set Username from GitHub",
                accountId,
                u -> u.addExternalId(externalIdFactory.create(key, accountId, null, null)));
      } catch (Exception e) {
        throw new IllegalArgumentException(
            "Internal error while trying to set username='" + username + "'");
      }

      log.debug(
          "Account {} updated with preferredEmail = {}, fullName = {}, username = {}",
          accountId,
          email,
          fullName,
          username);
    } catch (Exception e) {
      throw new ServletException(
          "Cannot associate email '" + email + "' to current user '" + user + "'", e);
    }
  }

  private void setAccoutPublicKeys(
      IdentifiedUser user, GitHubLogin hubLogin, HttpServletRequest req) throws IOException {
    GHMyself myself = hubLogin.getMyself();
    List<GHVerifiedKey> githubKeys = myself.getPublicVerifiedKeys();
    HashSet<String> gerritKeys = Sets.newHashSet(getCurrentGerritSshKeys(user));
    for (GHKey ghKey : githubKeys) {
      String sshKeyCheckedParam = "key_check_" + ghKey.getId();
      String sshKeyWithLabel = ghKey.getKey() + " " + ghKey.getTitle();
      String checked = req.getParameter(sshKeyCheckedParam);
      if (checked != null
          && checked.equalsIgnoreCase("on")
          && !gerritKeys.contains(ghKey.getKey())) {
        addSshKey(user, sshKeyWithLabel);
        gerritKeys.add(ghKey.getKey());
      }
    }
  }

  private List<String> getCurrentGerritSshKeys(final IdentifiedUser user) throws IOException {
    AccountResource res = new AccountResource(user);
    try {
      List<SshKeyInfo> keysInfo = restGetSshKeys.apply(res).value();
      return Lists.transform(
          keysInfo,
          new Function<SshKeyInfo, String>() {

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
    final ByteArrayInputStream keyIs = new ByteArrayInputStream(sshKeyWithLabel.getBytes());
    SshKeyInput key = new SshKeyInput();
    key.raw =
        new RawInput() {

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
