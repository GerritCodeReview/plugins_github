package com.googlesource.gerrit.plugins.github.wizard;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.kohsuke.github.GHKey;
import org.kohsuke.github.GHMyself;

import com.google.gerrit.extensions.restapi.RawInput;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.AccountResource;
import com.google.gerrit.server.account.AddSshKey;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.github.oauth.GitHubLogin;
import com.googlesource.gerrit.plugins.github.oauth.OAuthProtocol.Scope;

@Singleton
public class SshKeysImportServlet extends HttpServlet {
  private static final long serialVersionUID = 5565594120346641704L;
  private AddSshKey addSshKey;
  private Provider<GitHubLogin> loginProvider;
  private Provider<IdentifiedUser> userProvider;

  @Inject
  public SshKeysImportServlet(final AddSshKey addSshKey,
      Provider<GitHubLogin> loginProvider, Provider<IdentifiedUser> userProvider) {
    this.addSshKey = addSshKey;
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
    List<GHKey> pubKeys = myself.getPublicKeys();
    for (GHKey ghKey : pubKeys) {
      AccountResource res = new AccountResource(userProvider.get());
      AddSshKey.Input key = new AddSshKey.Input();
      final String sshKey = ghKey.getKey();
      final String sshKeyLabel = ghKey.getTitle();
      String sshKeyWithLabel = sshKey + " " + sshKeyLabel;
      out.println("Importing key " + sshKeyWithLabel);
      final ByteArrayInputStream keyIs =
          new ByteArrayInputStream(sshKeyWithLabel.getBytes());
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
          return sshKey.length();
        }
      };
      try {
        addSshKey.apply(res, key);
      } catch (Exception e) {
        throw new IOException("Cannot store SSH Key '" + sshKey + "'", e);
      }
    }
    } finally {
      out.println("</pre></body></html>");
      out.close();
    }
  }

}
