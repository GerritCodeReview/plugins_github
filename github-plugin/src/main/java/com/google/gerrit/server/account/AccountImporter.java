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
package com.google.gerrit.server.account;

import com.google.common.base.MoreObjects;
import com.google.gerrit.entities.Account;
import com.google.gerrit.server.ServerInitiated;
import com.google.gerrit.server.account.externalids.ExternalId;
import com.google.gerrit.server.account.externalids.ExternalIdFactory;
import com.google.gerrit.server.notedb.Sequences;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jgit.errors.ConfigInvalidException;

public class AccountImporter {
  private final Sequences sequences;
  private final Provider<AccountsUpdate> accountsUpdateProvider;
  private final ExternalIdFactory externalIdFactory;

  @Inject
  public AccountImporter(
      Sequences sequences,
      @ServerInitiated Provider<AccountsUpdate> accountsUpdateProvider,
      ExternalIdFactory externalIdFactory) {
    this.sequences = sequences;
    this.accountsUpdateProvider = accountsUpdateProvider;
    this.externalIdFactory = externalIdFactory;
  }

  public Account.Id importAccount(String login, String name, String email)
      throws IOException, ConfigInvalidException {
    Account.Id id = Account.id(sequences.nextAccountId());
    List<ExternalId> extIds = new ArrayList<>();
    extIds.add(externalIdFactory.createEmail(id, email));
    extIds.add(externalIdFactory.create(ExternalId.SCHEME_GERRIT, login, id));
    extIds.add(externalIdFactory.create(ExternalId.SCHEME_USERNAME, login, id));
    AccountState accountUpdate =
        accountsUpdateProvider
            .get()
            .insert(
                "Create GitHub account for " + login,
                id,
                u ->
                    u.setFullName(MoreObjects.firstNonNull(name, login))
                        .setPreferredEmail(email)
                        .addExternalIds(extIds));
    return accountUpdate.account().id();
  }
}
