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
package com.googlesource.gerrit.plugins.github;

import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.CONF_KEY_SECTION;
import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.KeyConfig.CIPHER_ALGORITHM_DEFAULT;
import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.KeyConfig.CIPHER_ALGO_CONFIG_LABEL;
import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.KeyConfig.CURRENT_CONFIG_LABEL;
import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.KeyConfig.IS_CURRENT_DEFAULT;
import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.KeyConfig.KEY_ID_DEFAULT;
import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.KeyConfig.PASSWORD_DEVICE_CONFIG_LABEL;
import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.KeyConfig.PASSWORD_LENGTH_CONFIG_LABEL;
import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.KeyConfig.PASSWORD_LENGTH_DEFAULT;
import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.KeyConfig.SECRET_KEY_ALGORITHM_DEFAULT;
import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.KeyConfig.SECRET_KEY_CONFIG_LABEL;
import static com.googlesource.gerrit.plugins.github.oauth.PasswordGenerator.DEFAULT_PASSWORD_FILE;

import com.google.common.base.Strings;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.client.AuthType;
import com.google.gerrit.pgm.init.api.ConsoleUI;
import com.google.gerrit.pgm.init.api.InitFlags;
import com.google.gerrit.pgm.init.api.InitStep;
import com.google.gerrit.pgm.init.api.InitUtil;
import com.google.gerrit.pgm.init.api.Section;
import com.google.gerrit.server.config.SitePaths;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.github.oauth.PasswordGenerator;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Optional;
import org.eclipse.jgit.storage.file.FileBasedConfig;

public class InitGitHub implements InitStep {
  private static final String GITHUB_URL = "https://github.com";
  private static final String GITHUB_API_URL = "https://api.github.com";
  private static final String GITHUB_REGISTER_APPLICATION_PATH = "/settings/applications/new";
  private static final String GERRIT_OAUTH_CALLBACK_PATH = "oauth";

  private final Path pluginData;
  private final ConsoleUI ui;
  private final Section auth;
  private final Section httpd;
  private final Section github;
  private final Section gerrit;
  private final Section.Factory sections;
  private final FileBasedConfig cfg;

  @Inject
  InitGitHub(
      @PluginName String pluginName,
      SitePaths site,
      final ConsoleUI ui,
      final Section.Factory sections,
      InitFlags flags) {
    this.pluginData = site.data_dir.resolve(pluginName);
    this.ui = ui;
    this.sections = sections;
    this.cfg = flags.cfg;
    this.github = sections.get("github", null);
    this.httpd = sections.get("httpd", null);
    this.auth = sections.get("auth", null);
    this.gerrit = sections.get("gerrit", null);
  }

  @Override
  public void run() throws Exception {
    ui.header("GitHub Integration");

    github.string("GitHub URL", "url", GITHUB_URL);
    github.string("GitHub API URL", "apiUrl", GITHUB_API_URL);
    ui.message(
        "\nNOTE: You might need to configure a proxy using http.proxy"
            + " if you run Gerrit behind a firewall.\n");

    String gerritUrl = getAssumedCanonicalWebUrl();
    ui.header("GitHub OAuth registration and credentials");
    ui.message(
        "Register Gerrit as GitHub application on:\n" + "%s%s\n\n",
        github.get("url"), GITHUB_REGISTER_APPLICATION_PATH);
    ui.message("Settings (assumed Gerrit URL: %s)\n", gerritUrl);
    ui.message("* Application name: Gerrit Code Review\n");
    ui.message("* Homepage URL: %s\n", gerritUrl);
    ui.message("* Authorization callback URL: %s%s\n\n", gerritUrl, GERRIT_OAUTH_CALLBACK_PATH);
    ui.message("After registration is complete, enter the generated OAuth credentials:\n");

    github.string("GitHub Client ID", "clientId", null);
    github.passwordForKey("GitHub Client Secret", "clientSecret");

    AuthType authType =
        auth.select(
            "Gerrit OAuth implementation",
            "type",
            AuthType.HTTP,
            EnumSet.of(AuthType.HTTP, AuthType.OAUTH));
    if (authType.equals(AuthType.HTTP)) {
      auth.string("HTTP Authentication Header", "httpHeader", "GITHUB_USER");
      httpd.set("filterClass", "com.googlesource.gerrit.plugins.github.oauth.OAuthFilter");
      authSetDefault("httpExternalIdHeader", "GITHUB_OAUTH_TOKEN");
      authSetDefault("loginUrl", "/login");
      authSetDefault("loginText", "Sign-in with GitHub");
      authSetDefault("registerPageUrl", "/#/register");
    } else {
      httpd.unset("filterClass");
      httpd.unset("httpHeader");
    }

    setupGitHubOAuthTokenCipher();
  }

  private void setupGitHubOAuthTokenCipher() {
    ui.header("GitHub OAuth token cipher configuration");
    Optional<String> maybeCurrentKeyId = getCurrentKeyId();

    boolean configureNewPasswordDevice = false;
    if (maybeCurrentKeyId.isPresent()) {
      if (!ui.yesno(
          false,
          "Current GitHub OAuth token cipher is configured under the %s key id. Do you want to"
              + " configure a new one?",
          maybeCurrentKeyId.get())) {
        return;
      }

      String notUniqueKeyIdPrefix = "K";
      do {
        String newKeyId = ui.readString("some-key-id", "%sey identifier", notUniqueKeyIdPrefix);
        if (cfg.getSubsections(CONF_KEY_SECTION).contains(newKeyId)) {
          notUniqueKeyIdPrefix =
              String.format(
                  "Provided key id '%s' already exists. Please provide a different key.", newKeyId);
          continue;
        }

        cfg.setBoolean(CONF_KEY_SECTION, maybeCurrentKeyId.get(), CURRENT_CONFIG_LABEL, false);
        maybeCurrentKeyId = Optional.of(newKeyId);
        configureNewPasswordDevice = true;
        break;
      } while (true);
    }

    String currentKeyId = maybeCurrentKeyId.orElse(KEY_ID_DEFAULT);
    ui.message("Configuring GitHub OAuth token cipher under '%s' key id\n", currentKeyId);
    Section gitHubKey = sections.get(CONF_KEY_SECTION, currentKeyId);

    gitHubKey.set(CURRENT_CONFIG_LABEL, "true");

    Path defaultPasswordPath = pluginData.resolve(DEFAULT_PASSWORD_FILE);
    String currentPasswordPath =
        gitHubKey.string(
            "Password file or device",
            PASSWORD_DEVICE_CONFIG_LABEL,
            configureNewPasswordDevice ? null : defaultPasswordPath.toString());
    if (defaultPasswordPath.toString().equalsIgnoreCase(currentPasswordPath)) {
      pluginData.toFile().mkdirs();
      if (new PasswordGenerator().generate(Path.of(currentPasswordPath))) {
        ui.message(
            "New password (%d bytes long) was generated under '%s' file.\n",
            PASSWORD_LENGTH_DEFAULT, currentPasswordPath);
      } else {
        ui.message(
            "The file under '%s' path already exists. Password wasn't regenerated.\n",
            currentPasswordPath);
      }
    } else {
      // ask for length only if default password is not used
      gitHubKey.set(
          PASSWORD_LENGTH_CONFIG_LABEL,
          String.valueOf(ui.readInt(PASSWORD_LENGTH_DEFAULT, "Password length in bytes")));
    }

    gitHubKey.string(
        "The algorithm to be used to encrypt the provided password",
        SECRET_KEY_CONFIG_LABEL,
        SECRET_KEY_ALGORITHM_DEFAULT);

    gitHubKey.string(
        "The algorithm to be used for encryption/decryption",
        CIPHER_ALGO_CONFIG_LABEL,
        CIPHER_ALGORITHM_DEFAULT);
  }

  private Optional<String> getCurrentKeyId() {
    return cfg.getSubsections(CONF_KEY_SECTION).stream()
        .filter(
            keyId ->
                cfg.getBoolean(CONF_KEY_SECTION, keyId, CURRENT_CONFIG_LABEL, IS_CURRENT_DEFAULT))
        .findFirst();
  }

  private void authSetDefault(String key, String defValue) {
    if (Strings.isNullOrEmpty(auth.get(key))) {
      auth.set(key, defValue);
    }
  }

  private String getAssumedCanonicalWebUrl() {
    String url = gerrit.get("canonicalWebUrl");
    if (url != null) {
      return url;
    }

    String httpListen = httpd.get("listenUrl");
    if (httpListen != null) {
      try {
        return InitUtil.toURI(httpListen).toString();
      } catch (URISyntaxException e) {
      }
    }

    return String.format("http://%s:8080/", InitUtil.hostname());
  }

  @Override
  public void postRun() throws Exception {}
}
