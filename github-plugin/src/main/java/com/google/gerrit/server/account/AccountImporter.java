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
import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.TopLevelResource;
import com.google.gerrit.extensions.restapi.UnprocessableEntityException;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.reviewdb.client.Account.Id;
import com.google.gerrit.reviewdb.client.AccountExternalId;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.account.CreateAccount.Factory;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.apache.http.HttpStatus;

import java.io.IOException;
import java.util.Arrays;

public class AccountImporter {
  private final Factory createAccountFactory;
  private final Provider<ReviewDb> schema;

  @Inject
  public AccountImporter(final CreateAccount.Factory createAccountFactory,
      final Provider<ReviewDb> schema) {
    this.createAccountFactory = createAccountFactory;
    this.schema = schema;
  }

  public Account.Id importAccount(String login, String name, String email)
      throws IOException, BadRequestException, ResourceConflictException,
      UnprocessableEntityException, OrmException {
    ReviewDb db = schema.get();
    CreateAccount createAccount = createAccountFactory.create(login);
    CreateAccount.Input accountInput = new CreateAccount.Input();
    accountInput.email = email;
    accountInput.username = login;
    accountInput.name = MoreObjects.firstNonNull(name, login);
    Response<AccountInfo> accountResponse =
        (Response<AccountInfo>) createAccount.apply(TopLevelResource.INSTANCE,
            accountInput);
    if (accountResponse.statusCode() == HttpStatus.SC_CREATED) {
      Id accountId = new Account.Id(accountResponse.value()._accountId);
      db.accountExternalIds().insert(
          Arrays
              .asList(new AccountExternalId(accountId,
                  new AccountExternalId.Key(AccountExternalId.SCHEME_GERRIT,
                      login))));
      return accountId;
    } else {
      throw new IOException("Cannot import GitHub account " + login
          + ": HTTP Status " + accountResponse.statusCode());
    }
  }
}
