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
package com.googlesource.gerrit.plugins.github.oauth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.github.GHEmail;
import org.kohsuke.github.GHMyself;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.gerrit.httpd.GitOverHttpServlet;
import com.google.gerrit.httpd.XGerritAuth;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.reviewdb.client.AccountExternalId;
import com.google.gerrit.reviewdb.server.AccountAccess;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gwtorm.server.OrmException;
import com.google.gwtorm.server.SchemaFactory;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class OpenId2OAuthFilter extends OAuthFilter {
  private static final org.slf4j.Logger log = LoggerFactory
      .getLogger(OpenId2OAuthFilter.class);

  private static final String PREFERRED_EMAIL_ERROR_PAGE
    = "/static/openid2oautherror.html";

  private final SchemaFactory<ReviewDb> schema;
  private final ScopedProvider<GitHubLogin> loginProvider;

  @Inject
  public OpenId2OAuthFilter(GitHubOAuthConfig config,
      OAuthWebFilter webFilter, Injector injector, SchemaFactory<ReviewDb> schema,
      GitHubLogin.Provider loginProvider) {
    super(config, webFilter, injector);

    this.schema = schema;
    this.loginProvider = loginProvider;
  }

  private boolean haveGitHubIdEntry(ReviewDb db, Account.Id accountId,
      AccountExternalId.Key key) throws OrmException {

    boolean found = false;
    for (AccountExternalId id : db.accountExternalIds().byAccount(accountId)) {
      if (id.getKey().equals(key)) {
        found = true;
        break;
      }
    }

    return found;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response,
      FilterChain chain) throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse= (HttpServletResponse) response;

//    String url = ((HttpServletRequest)request).getRequestURL().toString();
//    if (url.startsWith(PREFERRED_EMAIL_ERROR_PAGE)) {
//      return;
//    }

    GHMyself myself = this.loginProvider.get(httpRequest).getMyself();

    if (myself != null) {

      List<GHEmail> emails = myself.getEmails2();

      ReviewDb db = null;

      try {

        db = this.schema.open();
        AccountAccess accounts = db.accounts();

        for (GHEmail email: emails) {
          List<Account> results = accounts.byPreferredEmail(email.getEmail()).toList();

          if (results.size() > 1) {
            //httpResponse.sendRedirect(String.format("%s?email=%s", PREFERRED_EMAIL_ERROR_PAGE, email.getEmail()));
            log.error(String.format("'%s' is register as preferred email for multiple accounts", email.getEmail()));
            break;
          }

          Account.Id accountId =results.get(0).getId();

          AccountExternalId.Key gitHubKey = new AccountExternalId.Key(AccountExternalId.SCHEME_GERRIT,
                                              myself.getLogin());

          // Do we already have the entry
          if (haveGitHubIdEntry(db, accountId, gitHubKey)) {
            break;
          }

          log.info(String.format("Inserting external id entry for '%s' " +
              "who's preferred email '%s' matched their GitHub account.", myself.getLogin(), email.getEmail()));

          db.accountExternalIds().insert(
                Arrays
                .asList(new AccountExternalId(accountId, gitHubKey)));
          // Can only have a single account associated with a GitHub ID
          break;
        }
      }
      catch(com.google.gwtorm.server.OrmException orex) {
        log.error("Exception inserting GitHub external id entry", orex);
      }
      finally {
        if (db != null) {
          db.close();
        }
      }
    }

    super.doFilter(request, response, chain);

  }
}
