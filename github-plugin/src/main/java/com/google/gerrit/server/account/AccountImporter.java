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
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.server.Sequences;
import com.google.gerrit.server.ServerInitiated;
import com.google.gerrit.server.account.externalids.ExternalId;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jgit.errors.ConfigInvalidException;

public class AccountImporter {
  private final Sequences sequences;
  private final Provider<AccountsUpdate> accountsUpdateProvider;

  @Inject
  public AccountImporter(
      Sequences sequences, @ServerInitiated Provider<AccountsUpdate> accountsUpdateProvider) {
    this.sequences = sequences;
    this.accountsUpdateProvider = accountsUpdateProvider;
  }

  public Account.Id importAccount(String login, String name, String email)
      throws IOException, OrmException, ConfigInvalidException {
    Account.Id id = new Account.Id(sequences.nextAccountId());
    List<ExternalId> extIds = new ArrayList<>();
    extIds.add(ExternalId.createEmail(id, email));
    extIds.add(ExternalId.create(ExternalId.SCHEME_GERRIT, login, id));
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
    return accountUpdate.getAccount().getId();
  }
}
