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
import com.google.gerrit.extensions.api.accounts.SshKeyInput;
import com.google.gerrit.extensions.common.NameInput;
import com.google.gerrit.extensions.common.SshKeyInfo;
import com.google.gerrit.extensions.restapi.RawInput;
import com.google.gerrit.reviewdb.client.Account.Id;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.ServerInitiated;
import com.google.gerrit.server.account.externalids.ExternalId;
import com.google.gerrit.server.account.externalids.ExternalIds;
import com.google.gerrit.server.restapi.account.AddSshKey;
import com.google.gerrit.server.restapi.account.GetSshKeys;
import com.google.gerrit.server.restapi.account.PutName;
import com.google.gerrit.server.restapi.account.PutPreferred;
import com.google.gwtorm.server.OrmDuplicateKeyException;
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
import org.apache.commons.lang.StringUtils;
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

  @Inject
  public AccountController(
      final AddSshKey restAddSshKey,
      final GetSshKeys restGetSshKeys,
      final AccountManager accountManager,
      final AccountCache accountCache,
      final PutPreferred putPreferred,
      final PutName putName,
      @ServerInitiated final Provider<AccountsUpdate> accountsUpdateProvider,
      final ExternalIds externalIds) {
    this.restAddSshKey = restAddSshKey;
    this.restGetSshKeys = restGetSshKeys;
    this.accountManager = accountManager;
    this.accountCache = accountCache;
    this.putPreferred = putPreferred;
    this.putName = putName;
    this.accountsUpdateProvider = accountsUpdateProvider;
    this.externalIds = externalIds;
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

      log.info("Updated account '" + user.getUserName() + "'");
    } catch (IOException | ConfigInvalidException e) {
      log.error("Account '" + user.getUserName() + "' creation failed", e);
      throw new IOException(e);
    }
  }

  private void setAccountIdentity(IdentifiedUser user, HttpServletRequest req)
      throws ServletException, ConfigInvalidException {
    String fullName = req.getParameter("fullname");
    String email = req.getParameter("email");
    try {
      Id accountId = user.getAccountId();
      AuthResult result = accountManager.link(accountId, AuthRequest.forEmail(email));
      log.debug("Account {} linked to email {}: result = {}", accountId, email, result);

      putPreferred.apply(new AccountResource.Email(user, email), null);
      NameInput nameInput = new NameInput();
      nameInput.name = fullName;
      putName.apply(user, nameInput);

      Optional<String> optionalUsername = user.getUserName();
      optionalUsername.ifPresent(
          (String username) -> {
            ExternalId.Key key = ExternalId.Key.create(SCHEME_USERNAME, username);
            Optional<ExternalId> other = null;
            try {
              other = externalIds.get(key);
            } catch (IOException | ConfigInvalidException e) {
              throw new IllegalArgumentException(
                  "Internal error while fetching username='" + username + "'");
            }

            try {
              accountsUpdateProvider
                  .get()
                  .update(
                      "Set Username from GitHub",
                      accountId,
                      u -> u.addExternalId(ExternalId.create(key, accountId, null, null)));
            } catch (OrmDuplicateKeyException dupeErr) {
              // If we are using this identity, don't report the exception.
              if (!other.isPresent() || !other.get().accountId().equals(accountId)) {
                throw new IllegalArgumentException("username " + username + " already in use");
              }
            } catch (Exception e) {
              throw new IllegalArgumentException(
                  "Internal error while trying to set username='" + username + "'");
            }
          });

      log.debug(
          "Account {} updated with preferredEmail = {}, fullName = {}, username = {}",
          accountId,
          email,
          fullName,
          optionalUsername.orElse("<not set>"));

      accountCache.evict(accountId);
      log.debug("Account cache evicted for {}", accountId);
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
      List<SshKeyInfo> keysInfo = restGetSshKeys.apply(res);
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
