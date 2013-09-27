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

import java.io.IOException;

import org.apache.http.HttpStatus;
import org.kohsuke.github.GHUser;

import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.TopLevelResource;
import com.google.gerrit.extensions.restapi.UnprocessableEntityException;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.server.account.CreateAccount.Factory;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;

public class AccountImpoter {
  private Factory createAccountFactory;

  @Inject
  public AccountImpoter(CreateAccount.Factory createAccountFactory) {
    this.createAccountFactory = createAccountFactory;
  }

  public Account.Id importAccount(GHUser user) throws IOException,
      BadRequestException, ResourceConflictException,
      UnprocessableEntityException, OrmException {
    CreateAccount createAccount = createAccountFactory.create(user.getLogin());
    CreateAccount.Input accountInput = new CreateAccount.Input();
    accountInput.email = user.getEmail();
    accountInput.name = user.getName();
    accountInput.username = user.getLogin();
    Response<AccountInfo> accountResponse =
        (Response<AccountInfo>) createAccount.apply(TopLevelResource.INSTANCE,
            accountInput);
    if (accountResponse.statusCode() == HttpStatus.SC_CREATED) {
      return accountResponse.value()._id;
    } else {
      throw new IOException("Cannot import GitHub account " + user.getLogin()
          + ": HTTP Status " + accountResponse.statusCode());
    }
  }
}
