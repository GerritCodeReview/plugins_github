// Copyright (C) 2015 The Android Open Source Project
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

import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.reviewdb.client.AccountExternalId;
import com.google.gerrit.reviewdb.server.AccountAccess;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gwtorm.server.OrmException;
import com.google.gwtorm.server.SchemaFactory;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.github.GHEmail;
import org.kohsuke.github.GHMyself;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Singleton
public class OpenId2OAuthFilter extends OAuthFilter {
  private static final org.slf4j.Logger log = LoggerFactory
      .getLogger(OpenId2OAuthFilter.class);

  private static final String PREFERRED_EMAIL_ERROR_PAGE =
      "/static/openid2oautherror.html";

  private final SchemaFactory<ReviewDb> schema;
  private final ScopedProvider<GitHubLogin> loginProvider;

  @Inject
  public OpenId2OAuthFilter(GitHubOAuthConfig config,
      OAuthWebFilter webFilter,
      Injector injector,
      SchemaFactory<ReviewDb> schema,
      GitHubLogin.Provider loginProvider) {
    super(config, webFilter, injector);

    this.schema = schema;
    this.loginProvider = loginProvider;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response,
      FilterChain chain) throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    GHMyself myself = this.loginProvider.get(httpRequest).getMyself();

    if (myself != null) {

      ReviewDb db = null;

      try {
        db = this.schema.open();
        AccountExternalId.Key gitHubKey = new AccountExternalId.Key(AccountExternalId.SCHEME_GERRIT,
                myself.getLogin());

        // First check is we already have an external id entry for this account.
        if(db.accountExternalIds().get(gitHubKey) == null) {
          AccountAccess accounts = db.accounts();
          Set<String> matchingEmails = new HashSet<String>();
          List<Account> matchingAccounts = new ArrayList<Account>();

          // Try to find a matching Gerrit account using all verified GitHub
          // email adddresses.
          for (GHEmail email: myself.getEmails2()) {

            // Only search using verified email addresses, to stop people hijacking
            // someone's Gerrit account by added the email address to their GitHub
            // account.
            if (!email.isVerified()) {
              continue;
            }

            List<Account> results = accounts.byPreferredEmail(email.getEmail()).toList();

            if(!results.isEmpty()) {
              matchingAccounts.addAll(results);
              matchingEmails.add(email.getEmail());
            }
          }

          // We have a match
          if(matchingAccounts.size() == 1) {
            String email = matchingEmails.iterator().next();
            Account.Id accountId = matchingAccounts.get(0).getId();
            log.info(String.format("Inserting external id entry for '%s' " +
                    "whose preferred email '%s' matched their GitHub account.", myself.getLogin(), email));
            db.accountExternalIds().insert(
                    Arrays
                    .asList(new AccountExternalId(accountId, gitHubKey)));
          }
          // GitHub account matches multiple Gerrit accounts
          else if(matchingAccounts.size() > 1) {
            this.loginProvider.get(httpRequest).logout();
            String addresses = StringUtils.join(matchingEmails.toArray(), ",");
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.sendRedirect(String.format("%s?user=%s&emails=%s", PREFERRED_EMAIL_ERROR_PAGE, myself.getLogin(), addresses));
            log.error(String.format("GitHub user '%s' has multiple Gerrit accounts that match these registered email addresses '%s'",
                  myself.getLogin(), addresses));
            return;
          }
        }
      }
      catch(OrmException orex) {
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
