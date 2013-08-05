package com.googlesource.gerrit.plugins.github.wizard;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;

import javax.annotation.Nullable;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.kohsuke.github.GHKey;
import org.kohsuke.github.GHMyself;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.RawInput;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.AccountResource;
import com.google.gerrit.server.account.AddSshKey;
import com.google.gerrit.server.account.GetSshKeys;
import com.google.gerrit.server.account.GetSshKeys.SshKeyInfo;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.github.oauth.GitHubLogin;
import com.googlesource.gerrit.plugins.github.oauth.OAuthProtocol.Scope;

@Singleton
public class SshKeysImportServlet extends HttpServlet {
  private static final long serialVersionUID = 5565594120346641704L;
  private static final Logger log = LoggerFactory.getLogger(SshKeysImportServlet.class);
  private final AddSshKey restAddSshKey;
  private final GetSshKeys restGetSshKeys;
  private Provider<GitHubLogin> loginProvider;
  private Provider<IdentifiedUser> userProvider;

  @Inject
  public SshKeysImportServlet(final AddSshKey restAddSshKey,
      final GetSshKeys restGetSshKeys,
      Provider<GitHubLogin> loginProvider, Provider<IdentifiedUser> userProvider) {
    this.restAddSshKey = restAddSshKey;
    this.restGetSshKeys = restGetSshKeys;
    this.loginProvider = loginProvider;
    this.userProvider = userProvider;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    GitHubLogin hubLogin = loginProvider.get();
    if (!hubLogin.login(req, resp, Scope.USER)) {
      return;
    }

    PrintWriter out = resp.getWriter();
    out.println("<html><body><pre>");
    try {

      GHMyself myself = hubLogin.getMyself();
      List<GHKey> githubKeys = myself.getPublicKeys();
      HashSet<String> gerritKeys = Sets.newHashSet(getSshKeys());
      for (GHKey ghKey : githubKeys) {
        final String sshKey = ghKey.getKey();
        final String sshKeyLabel = ghKey.getTitle();
        final String sshKeyWithLabel = sshKey + " " + sshKeyLabel;
        
        if (!gerritKeys.contains(sshKeyWithLabel)) {
          out.println("Importing key " + sshKeyWithLabel);
          addSshKey(sshKeyWithLabel);
        }
      }
    } finally {
      out.println("</pre></body></html>");
      out.close();
    }
  }

  private List<String> getSshKeys() throws IOException {
    AccountResource res = newAccountResource();
    try {
      List<SshKeyInfo> keysInfo = restGetSshKeys.apply(res);
      return Lists.transform(keysInfo, new Function<SshKeyInfo, String>() {

        @Override
        @Nullable
        public String apply(@Nullable SshKeyInfo keyInfo) {
          return keyInfo.sshPublicKey;
        }

      });
    } catch (Exception e) {
      log.error("User list keys failed", e);
      throw new IOException("Cannot get list of user keys", e);
    }
  }

  private void addSshKey(final String sshKeyWithLabel) throws IOException {
    AccountResource res = newAccountResource();
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

  private AccountResource newAccountResource() {
    AccountResource res = new AccountResource(userProvider.get());
    return res;
  }

}
