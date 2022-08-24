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

import com.google.common.base.CharMatcher;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.gerrit.extensions.client.AuthType;
import com.google.gerrit.httpd.CanonicalWebUrl;
import com.google.gerrit.server.config.ConfigUtil;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.github.oauth.OAuthProtocol.Scope;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.eclipse.jgit.lib.Config;

@Singleton
public class GitHubOAuthConfig {
  private final Config config;
  private final CanonicalWebUrl canonicalWebUrl;

  public static final String CONF_SECTION = "github";
  public static final String CONF_KEY_SECTION = "github-keys";
  public static final String GITHUB_OAUTH_AUTHORIZE = "/login/oauth/authorize";
  public static final String GITHUB_OAUTH_ACCESS_TOKEN = "/login/oauth/access_token";
  public static final String GERRIT_OAUTH_FINAL = "/oauth";
  public static final String GITHUB_URL_DEFAULT = "https://github.com";
  public static final String GITHUB_API_URL_DEFAULT = "https://api.github.com";
  public static final String GERRIT_LOGIN = "/login";
  public static final String GERRIT_LOGOUT = "/logout";
  public static final String GITHUB_PLUGIN_OAUTH_SCOPE = "/plugins/github-plugin/static/scope.html";

  public final String gitHubUrl;
  public final String gitHubApiUrl;
  public final String gitHubClientId;
  public final String gitHubClientSecret;
  public final String logoutRedirectUrl;
  public final String httpHeader;
  public final String gitHubOAuthUrl;
  public final String gitHubOAuthAccessTokenUrl;
  public final boolean enabled;

  @Getter public final Map<ScopeKey, List<OAuthProtocol.Scope>> scopes;
  @Getter public final List<ScopeKey> sortedScopesKeys;

  public final int fileUpdateMaxRetryCount;
  public final int fileUpdateMaxRetryIntervalMsec;
  public final String oauthHttpHeader;

  public final long httpConnectionTimeout;
  public final long httpReadTimeout;
  private final Map<String, KeyConfig> keyConfigMap;
  private final KeyConfig currentKeyConfig;

  @Inject
  protected GitHubOAuthConfig(@GerritServerConfig Config config, CanonicalWebUrl canonicalWebUrl) {
    this.config = config;
    this.canonicalWebUrl = canonicalWebUrl;

    httpHeader =
        Preconditions.checkNotNull(
            config.getString("auth", null, "httpHeader"),
            "HTTP Header for GitHub user must be provided");
    gitHubUrl =
        trimTrailingSlash(
            MoreObjects.firstNonNull(
                config.getString(CONF_SECTION, null, "url"), GITHUB_URL_DEFAULT));
    gitHubApiUrl =
        trimTrailingSlash(
            MoreObjects.firstNonNull(
                config.getString(CONF_SECTION, null, "apiUrl"), GITHUB_API_URL_DEFAULT));
    gitHubClientId =
        Preconditions.checkNotNull(
            config.getString(CONF_SECTION, null, "clientId"), "GitHub `clientId` must be provided");
    gitHubClientSecret =
        Preconditions.checkNotNull(
            config.getString(CONF_SECTION, null, "clientSecret"),
            "GitHub `clientSecret` must be provided");

    oauthHttpHeader = config.getString("auth", null, "httpExternalIdHeader");
    gitHubOAuthUrl = gitHubUrl + GITHUB_OAUTH_AUTHORIZE;
    gitHubOAuthAccessTokenUrl = gitHubUrl + GITHUB_OAUTH_ACCESS_TOKEN;
    logoutRedirectUrl = config.getString(CONF_SECTION, null, "logoutRedirectUrl");

    enabled = config.getString("auth", null, "type").equalsIgnoreCase(AuthType.HTTP.toString());
    scopes = getScopes(config);
    sortedScopesKeys =
        scopes.keySet().stream()
            .sorted(Comparator.comparing(ScopeKey::getSequence))
            .collect(Collectors.toList());

    fileUpdateMaxRetryCount = config.getInt(CONF_SECTION, "fileUpdateMaxRetryCount", 3);
    fileUpdateMaxRetryIntervalMsec =
        config.getInt(CONF_SECTION, "fileUpdateMaxRetryIntervalMsec", 3000);

    httpConnectionTimeout =
        TimeUnit.MILLISECONDS.convert(
            ConfigUtil.getTimeUnit(
                config, CONF_SECTION, null, "httpConnectionTimeout", 30, TimeUnit.SECONDS),
            TimeUnit.SECONDS);

    httpReadTimeout =
        TimeUnit.MILLISECONDS.convert(
            ConfigUtil.getTimeUnit(
                config, CONF_SECTION, null, "httpReadTimeout", 30, TimeUnit.SECONDS),
            TimeUnit.SECONDS);

    Map<String, KeyConfig> configuredKeyConfig =
        config.getSubsections(CONF_KEY_SECTION).stream()
            .map(KeyConfig::new)
            .collect(Collectors.toMap(KeyConfig::getPrefix, Function.identity()));

    if (configuredKeyConfig.isEmpty()) {
      currentKeyConfig = new KeyConfig();
      keyConfigMap = Map.of(currentKeyConfig.getPrefix(), currentKeyConfig);
    } else {
      keyConfigMap = configuredKeyConfig;
      List<KeyConfig> currentKeyConfigs =
          keyConfigMap.values().stream().filter(KeyConfig::isCurrent).collect(Collectors.toList());
      if (currentKeyConfigs.size() != 1) {
        throw new IllegalStateException(
            String.format(
                "Expected exactly 1 subsection of '%s' to be configured as 'current', %d found",
                CONF_KEY_SECTION, currentKeyConfigs.size()));
      }
      currentKeyConfig = currentKeyConfigs.get(0);
    }
  }

  public String getOAuthFinalRedirectUrl(HttpServletRequest req) {
    return req == null
        ? GERRIT_OAUTH_FINAL
        : trimTrailingSlash(canonicalWebUrl.get(req)) + GERRIT_OAUTH_FINAL;
  }

  public String getScopeSelectionUrl(HttpServletRequest req) {
    String canonicalUrl = req == null ? "" : trimTrailingSlash(canonicalWebUrl.get(req));
    return canonicalUrl
        + MoreObjects.firstNonNull(
            config.getString(CONF_SECTION, null, "scopeSelectionUrl"), GITHUB_PLUGIN_OAUTH_SCOPE);
  }

  private Map<ScopeKey, List<Scope>> getScopes(Config config) {
    return config.getNames(CONF_SECTION, true).stream()
        .filter(k -> k.startsWith("scopes"))
        .filter(k -> !k.endsWith("Description"))
        .filter(k -> !k.endsWith("Sequence"))
        .collect(
            Collectors.toMap(
                k ->
                    new ScopeKey(
                        k,
                        config.getString(CONF_SECTION, null, k + "Description"),
                        config.getInt(CONF_SECTION, k + "Sequence", 0)),
                v -> parseScopesString(config.getString(CONF_SECTION, null, v))));
  }

  private String trimTrailingSlash(String url) {
    return CharMatcher.is('/').trimTrailingFrom(url);
  }

  private List<Scope> parseScopesString(String scopesString) {
    ArrayList<Scope> result = new ArrayList<>();
    if (Strings.emptyToNull(scopesString) != null) {
      String[] scopesStrings = scopesString.split(",");
      for (String scope : scopesStrings) {
        result.add(Enum.valueOf(Scope.class, scope.trim()));
      }
    }

    return result;
  }

  public Scope[] getDefaultScopes() {
    if (scopes == null || scopes.get("scopes") == null) {
      return new Scope[0];
    }
    return scopes.get("scopes").toArray(new Scope[0]);
  }

  public KeyConfig getCurrentKeyConfig() {
    return currentKeyConfig;
  }

  public KeyConfig getKeyConfig(String subsection) {
    return keyConfigMap.get(subsection);
  }

  public class KeyConfig {

    public static final String PASSWORD_DEVICE_DEFAULT = "/dev/zero";
    public static final int PASSWORD_LENGTH_DEFAULT = 16;
    public static final String CIPHER_ALGORITHM_DEFAULT = "AES/ECB/PKCS5Padding";
    public static final String SECRET_KEY_ALGORITHM_DEFAULT = "AES";
    public static final boolean IS_CURRENT_DEFAULT = false;
    public static final String KEY_PREFIX_DEFAULT = "current";

    public static final String PASSWORD_DEVICE_CONFIG_LABEL = "passwordDevice";
    public static final String PASSWORD_LENGTH_CONFIG_LABEL = "passwordLength";
    public static final String SECRET_KEY_CONFIG_LABEL = "secretKeyAlgorithm";
    public static final String CIPHER_ALGO_CONFIG_LABEL = "cipherAlgorithm";
    public static final String CURRENT_CONFIG_LABEL = "current";

    private final String passwordDevice;
    private final Integer passwordLength;
    private final String cipherAlgorithm;
    private final String secretKeyAlgorithm;
    private final String prefix;
    private final Boolean isCurrent;

    KeyConfig(String prefix) {

      this.passwordDevice =
          trimTrailingSlash(
              MoreObjects.firstNonNull(
                  config.getString(CONF_KEY_SECTION, prefix, PASSWORD_DEVICE_CONFIG_LABEL),
                  PASSWORD_DEVICE_DEFAULT));
      this.passwordLength =
          config.getInt(
              CONF_KEY_SECTION, prefix, PASSWORD_LENGTH_CONFIG_LABEL, PASSWORD_LENGTH_DEFAULT);
      isCurrent =
          config.getBoolean(CONF_KEY_SECTION, prefix, CURRENT_CONFIG_LABEL, IS_CURRENT_DEFAULT);

      this.cipherAlgorithm =
          trimTrailingSlash(
              MoreObjects.firstNonNull(
                  config.getString(CONF_KEY_SECTION, prefix, CIPHER_ALGO_CONFIG_LABEL),
                  CIPHER_ALGORITHM_DEFAULT));

      this.secretKeyAlgorithm =
          trimTrailingSlash(
              MoreObjects.firstNonNull(
                  config.getString(CONF_KEY_SECTION, prefix, SECRET_KEY_CONFIG_LABEL),
                  SECRET_KEY_ALGORITHM_DEFAULT));
      this.prefix = prefix;
    }

    private KeyConfig() {
      passwordDevice = PASSWORD_DEVICE_DEFAULT;
      passwordLength = PASSWORD_LENGTH_DEFAULT;
      isCurrent = true;
      cipherAlgorithm = CIPHER_ALGORITHM_DEFAULT;
      prefix = KEY_PREFIX_DEFAULT;
      secretKeyAlgorithm = SECRET_KEY_ALGORITHM_DEFAULT;
    }

    public byte[] readPassword() throws IOException {
      Path devicePath = Paths.get(passwordDevice);
      try (FileInputStream in = new FileInputStream(devicePath.toFile())) {
        byte[] passphrase = new byte[passwordLength];
        if (in.read(passphrase) < passwordLength) {
          throw new IOException("End of password device has already been reached");
        }

        return passphrase;
      }
    }

    public String getCipherAlgorithm() {
      return cipherAlgorithm;
    }

    public String getSecretKeyAlgorithm() {
      return secretKeyAlgorithm;
    }

    public Boolean isCurrent() {
      return isCurrent;
    }

    public String getPrefix() {
      return prefix;
    }
  }
}
