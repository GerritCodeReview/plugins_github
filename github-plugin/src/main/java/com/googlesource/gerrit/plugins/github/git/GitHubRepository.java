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
package com.googlesource.gerrit.plugins.github.git;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import com.googlesource.gerrit.plugins.github.GitHubURL;
import com.googlesource.gerrit.plugins.github.oauth.GitHubLogin;
import com.googlesource.gerrit.plugins.github.oauth.ScopedProvider;

import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.IOException;

import lombok.experimental.Delegate;

public class GitHubRepository extends GHRepository {
  public interface Factory {
    GitHubRepository create(@Assisted("organisation") String organisation,
        @Assisted("repository") String repository);
  }


  private final String organisation;
  private final String repository;
  private final String cloneUrl;
  private final String username;
  private final String password;

  @Delegate
  private GHRepository ghRepository;

  public String getCloneUrl() {
    return cloneUrl.replace("://", "://" + username + "@");
  }

  public String getOrganisation() {
    return organisation;
  }

  public String getRepository() {
    return repository;
  }

  @Inject
  public GitHubRepository(ScopedProvider<GitHubLogin> ghLoginProvider,
      @GitHubURL String gitHubUrl,
      @Assisted("organisation") String organisation,
      @Assisted("repository") String repository) throws IOException {
    this.cloneUrl = gitHubUrl + "/" + organisation + "/" + repository + ".git";
    this.organisation = organisation;
    this.repository = repository;
    GitHubLogin ghLogin = ghLoginProvider.get();
    GitHub gh = ghLogin.getHub();
    this.username = ghLogin.getMyself().getLogin();
    this.password = ghLogin.getToken().accessToken;
    this.ghRepository =
        gh.getRepository(organisation + "/" + repository);
  }

  public CredentialsProvider getCredentialsProvider() {
    return new CredentialsProvider() {

      @Override
      public boolean supports(CredentialItem... items) {
        for (CredentialItem i : items) {
          if (i instanceof CredentialItem.Username) {
            continue;
          } else if (i instanceof CredentialItem.Password) {
            continue;
          } else {
            return false;
          }
        }
        return true;
      }

      @Override
      public boolean isInteractive() {
        return false;
      }

      @Override
      public boolean get(URIish uri, CredentialItem... items)
          throws UnsupportedCredentialItem {
        String _username = uri.getUser();
        if (_username == null) {
          _username = GitHubRepository.this.username;
        }
        if (_username == null) {
          return false;
        }

        String _password = GitHubRepository.this.password;
        if (password == null) {
          return false;
        }

        for (CredentialItem i : items) {
          if (i instanceof CredentialItem.Username) {
            ((CredentialItem.Username) i).setValue(_username);
          } else if (i instanceof CredentialItem.Password) {
            ((CredentialItem.Password) i).setValue(_password.toCharArray());
          } else {
            throw new UnsupportedCredentialItem(uri, i.getPromptText());
          }
        }
        return true;
      }
    };
  }
}
