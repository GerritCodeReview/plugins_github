// Copyright (C) 2022 The Android Open Source Project
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

import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.CONF_KEY_SECTION;
import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.CONF_SECTION;
import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.KeyConfig.CIPHER_ALGO_CONFIG_LABEL;
import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.KeyConfig.CURRENT_CONFIG_LABEL;
import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.KeyConfig.KEY_DELIMITER;
import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.KeyConfig.PASSWORD_DEVICE_CONFIG_LABEL;
import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.KeyConfig.SECRET_KEY_CONFIG_LABEL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.google.gerrit.extensions.client.AuthType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.eclipse.jgit.lib.Config;
import org.junit.Before;
import org.junit.Test;

public class GitHubOAuthConfigTest {

  Config config;
  private static final String testPasswordDevice = "/dev/zero";

  @Before
  public void setUp() {
    config = new Config();
    config.setString(CONF_SECTION, null, "clientSecret", "theSecret");
    config.setString(CONF_SECTION, null, "clientId", "theClientId");
    config.setString("auth", null, "httpHeader", "GITHUB_USER");
    config.setString("auth", null, "type", AuthType.HTTP.toString());
  }

  @Test
  public void shouldReadASpecificKeyConfig() {
    String keySubsection = "someKeyConfig";
    String cipherAlgorithm = "AES/CFB8/NoPadding";
    String secretKeyAlgorithm = "DES";
    config.setBoolean(CONF_KEY_SECTION, keySubsection, CURRENT_CONFIG_LABEL, true);
    config.setString(
        CONF_KEY_SECTION, keySubsection, PASSWORD_DEVICE_CONFIG_LABEL, testPasswordDevice);
    config.setString(CONF_KEY_SECTION, keySubsection, CIPHER_ALGO_CONFIG_LABEL, cipherAlgorithm);
    config.setString(CONF_KEY_SECTION, keySubsection, SECRET_KEY_CONFIG_LABEL, secretKeyAlgorithm);

    assertEquals(githubOAuthConfig().getCurrentKeyConfig().isCurrent(), true);
    assertEquals(githubOAuthConfig().getCurrentKeyConfig().getCipherAlgorithm(), cipherAlgorithm);
    assertEquals(githubOAuthConfig().getCurrentKeyConfig().getSecretKeyAlgorithm(), secretKeyAlgorithm);
    assertEquals(githubOAuthConfig().getCurrentKeyConfig().getKeyId(), keySubsection);
  }

  @Test
  public void shouldReturnTheExpectedKeyConfigAsCurrent() {
    String currentKeyConfig = "currentKeyConfig";
    String someOtherKeyConfig = "someOtherKeyConfig";
    config.setBoolean(CONF_KEY_SECTION, currentKeyConfig, CURRENT_CONFIG_LABEL, true);
    config.setString(
        CONF_KEY_SECTION, currentKeyConfig, PASSWORD_DEVICE_CONFIG_LABEL, testPasswordDevice);
    config.setBoolean(CONF_KEY_SECTION, someOtherKeyConfig, CURRENT_CONFIG_LABEL, false);
    config.setString(
        CONF_KEY_SECTION, someOtherKeyConfig, PASSWORD_DEVICE_CONFIG_LABEL, testPasswordDevice);

    assertEquals(githubOAuthConfig().getCurrentKeyConfig().getKeyId(), currentKeyConfig);
  }

  @Test
  public void shouldReadMultipleKeyConfigs() {
    String currentKeyConfig = "currentKeyConfig";
    String someOtherKeyConfig = "someOtherKeyConfig";
    config.setBoolean(CONF_KEY_SECTION, currentKeyConfig, CURRENT_CONFIG_LABEL, true);
    config.setString(
        CONF_KEY_SECTION, currentKeyConfig, PASSWORD_DEVICE_CONFIG_LABEL, testPasswordDevice);
    config.setBoolean(CONF_KEY_SECTION, someOtherKeyConfig, CURRENT_CONFIG_LABEL, false);
    config.setString(
        CONF_KEY_SECTION, someOtherKeyConfig, PASSWORD_DEVICE_CONFIG_LABEL, testPasswordDevice);

    assertEquals(githubOAuthConfig().getKeyConfig(currentKeyConfig).getKeyId(), currentKeyConfig);
    assertEquals(githubOAuthConfig().getKeyConfig(someOtherKeyConfig).getKeyId(), someOtherKeyConfig);
  }

  @Test
  public void shouldThrowWhenNoKeyIdIsConfigured() {
    IllegalStateException illegalStateException =
        assertThrows(IllegalStateException.class, this::githubOAuthConfig);

    assertEquals(
        illegalStateException.getMessage(),
        String.format(
            "Expected exactly 1 subsection of '%s' to be configured as 'current', %d found",
            CONF_KEY_SECTION, 0));
  }

  @Test
  public void shouldThrowWhenNoKeyConfigIsSetAsCurrent() {
    config.setBoolean(CONF_KEY_SECTION, "someKeyConfig", CURRENT_CONFIG_LABEL, false);

    assertThrows(IllegalStateException.class, this::githubOAuthConfig);
  }

  @Test
  public void shouldThrowWhenKeyConfigContainsDelimiterCharacter() {
    String invalidSubsection = "foo" + KEY_DELIMITER + "bar";
    config.setBoolean(CONF_KEY_SECTION, invalidSubsection, CURRENT_CONFIG_LABEL, false);

    IllegalStateException illegalStateException =
        assertThrows(IllegalStateException.class, this::githubOAuthConfig);

    assertEquals(
        illegalStateException.getMessage(),
        String.format(
            "Configuration error. %s.%s should not contain '%s'",
            CONF_KEY_SECTION, invalidSubsection, KEY_DELIMITER));
  }

  @Test
  public void shouldThrowWhenMoreThanOneKeyConfigIsSetAsCurrent() {
    config.setBoolean(CONF_KEY_SECTION, "someKeyConfig", CURRENT_CONFIG_LABEL, true);
    config.setBoolean(CONF_KEY_SECTION, "someOtherKeyConfig", CURRENT_CONFIG_LABEL, true);

    assertThrows(IllegalStateException.class, this::githubOAuthConfig);
  }

  @Test
  public void shouldThrowWhenKeyIdMissesPasswordDevice() {
    String someKeyConfig = "someKeyConfig";
    config.setBoolean(CONF_KEY_SECTION, someKeyConfig, CURRENT_CONFIG_LABEL, true);

    IllegalStateException illegalStateException =
        assertThrows(IllegalStateException.class, this::githubOAuthConfig);

    assertEquals(
        String.format(
            "Configuration error. Missing %s.%s for key-id '%s'",
            CONF_KEY_SECTION, PASSWORD_DEVICE_CONFIG_LABEL, someKeyConfig),
        illegalStateException.getMessage());
  }

  @Test
  public void shouldReturnEmptyCookieDomainByDefault() {
    setupEncryptionConfig();
    assertEquals(Optional.empty(), githubOAuthConfig().getCookieDomain());
  }

  @Test
  public void shouldReturnTheCookieDomainFromAuth() {
    setupEncryptionConfig();
    String myDomain = ".mydomain.com";
    config.setString("auth", null, "cookieDomain", myDomain);

    assertEquals(Optional.of(myDomain), githubOAuthConfig().getCookieDomain());
  }

  @Test
  public void shouldReturnOverridesForSpecificHostName() {
    setupEncryptionConfig();
    String vhost = "v.host.com";
    String description = "scope description";

    // virtual host scopes
    config.setString(CONF_SECTION, vhost, "scopesVHost", "USER_EMAIL");
    config.setInt(CONF_SECTION, vhost, "scopesVHostSequence", 1);
    config.setString(CONF_SECTION, vhost, "scopesVHostDescription", "scope description");

    Map<String, Map<ScopeKey, List<OAuthProtocol.Scope>>> virtualScopes =
        githubOAuthConfig().getVirtualScopes();

    assertEquals(virtualScopes.containsKey(vhost), true);

    Map<ScopeKey, List<OAuthProtocol.Scope>> vhostConfig = virtualScopes.get(vhost);
    Map.Entry<ScopeKey, List<OAuthProtocol.Scope>> entry = vhostConfig.entrySet().iterator().next();

    assertEquals(entry.getKey().name, "scopesVHost");
    assertEquals(entry.getKey().description, description);
    assertEquals(entry.getKey().sequence, 1);
    assertEquals(List.of(OAuthProtocol.Scope.USER_EMAIL), entry.getValue());
  }

  private GitHubOAuthConfig githubOAuthConfig() {
    return new GitHubOAuthConfig(config);
  }

  private void setupEncryptionConfig() {
    String keySubsection = "someKeyConfig";
    String cipherAlgorithm = "AES/CFB8/NoPadding";
    String secretKeyAlgorithm = "DES";
    config.setBoolean(CONF_KEY_SECTION, keySubsection, CURRENT_CONFIG_LABEL, true);
    config.setString(
        CONF_KEY_SECTION, keySubsection, PASSWORD_DEVICE_CONFIG_LABEL, testPasswordDevice);
    config.setString(CONF_KEY_SECTION, keySubsection, CIPHER_ALGO_CONFIG_LABEL, cipherAlgorithm);
    config.setString(CONF_KEY_SECTION, keySubsection, SECRET_KEY_CONFIG_LABEL, secretKeyAlgorithm);
  }
}
