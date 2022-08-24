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
import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.KeyConfig.CIPHER_ALGORITHM_DEFAULT;
import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.KeyConfig.CIPHER_ALGO_CONFIG_LABEL;
import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.KeyConfig.CURRENT_CONFIG_LABEL;
import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.KeyConfig.KEY_DELIMITER;
import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.KeyConfig.KEY_ID_DEFAULT;
import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.KeyConfig.SECRET_KEY_ALGORITHM_DEFAULT;
import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.KeyConfig.SECRET_KEY_CONFIG_LABEL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.google.gerrit.extensions.client.AuthType;
import com.google.gerrit.httpd.CanonicalWebUrl;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.util.Providers;
import org.eclipse.jgit.lib.Config;
import org.junit.Before;
import org.junit.Test;

public class GitHubOAuthConfigTest {

  CanonicalWebUrl canonicalWebUrl;
  Config config;

  @Before
  public void setUp() {
    config = new Config();
    config.setString(CONF_SECTION, null, "clientSecret", "theSecret");
    config.setString(CONF_SECTION, null, "clientId", "theClientId");
    config.setString("auth", null, "httpHeader", "GITHUB_USER");
    config.setString("auth", null, "type", AuthType.HTTP.toString());

    canonicalWebUrl =
        Guice.createInjector(
                new AbstractModule() {
                  @Override
                  protected void configure() {
                    bind(String.class)
                        .annotatedWith(com.google.gerrit.server.config.CanonicalWebUrl.class)
                        .toProvider(Providers.of(null));
                  }
                })
            .getInstance(CanonicalWebUrl.class);
  }

  @Test
  public void shouldDefaultKeyConfigWhenNoSpecificConfigurationIsSet() {
    GitHubOAuthConfig objectUnderTest = objectUnderTest();
    assertEquals(objectUnderTest.getCurrentKeyConfig().isCurrent(), true);
    assertEquals(
        objectUnderTest.getCurrentKeyConfig().getCipherAlgorithm(), CIPHER_ALGORITHM_DEFAULT);
    assertEquals(
        objectUnderTest.getCurrentKeyConfig().getSecretKeyAlgorithm(),
        SECRET_KEY_ALGORITHM_DEFAULT);
    assertEquals(objectUnderTest.getCurrentKeyConfig().getKeyId(), KEY_ID_DEFAULT);
  }

  @Test
  public void shouldDBeAbleToLookupDefaultKeyWhenNoSpecificConfigurationIsSet() {
    assertEquals(objectUnderTest().getKeyConfig(KEY_ID_DEFAULT).isCurrent(), true);
  }

  @Test
  public void shouldReadASpecificKeyConfig() {
    String keySubsection = "someKeyConfig";
    String cipherAlgorithm = "AES/CFB8/NoPadding";
    String secretKeyAlgorithm = "DES";
    config.setBoolean(CONF_KEY_SECTION, keySubsection, CURRENT_CONFIG_LABEL, true);
    config.setString(CONF_KEY_SECTION, keySubsection, CIPHER_ALGO_CONFIG_LABEL, cipherAlgorithm);
    config.setString(CONF_KEY_SECTION, keySubsection, SECRET_KEY_CONFIG_LABEL, secretKeyAlgorithm);

    GitHubOAuthConfig objectUnderTest = objectUnderTest();

    assertEquals(objectUnderTest.getCurrentKeyConfig().isCurrent(), true);
    assertEquals(objectUnderTest.getCurrentKeyConfig().getCipherAlgorithm(), cipherAlgorithm);
    assertEquals(objectUnderTest.getCurrentKeyConfig().getSecretKeyAlgorithm(), secretKeyAlgorithm);
    assertEquals(objectUnderTest.getCurrentKeyConfig().getKeyId(), keySubsection);
  }

  @Test
  public void shouldReturnTheExpectedKeyConfigAsCurrent() {
    config.setBoolean(CONF_KEY_SECTION, "currentKeyConfig", CURRENT_CONFIG_LABEL, true);
    config.setBoolean(CONF_KEY_SECTION, "someOtherKeyConfig", CURRENT_CONFIG_LABEL, false);

    assertEquals(objectUnderTest().getCurrentKeyConfig().getKeyId(), "currentKeyConfig");
  }

  @Test
  public void shouldReadMultipleKeyConfigs() {
    String currentKeyConfig = "currentKeyConfig";
    String someOtherKeyConfig = "someOtherKeyConfig";
    config.setBoolean(CONF_KEY_SECTION, currentKeyConfig, CURRENT_CONFIG_LABEL, true);
    config.setBoolean(CONF_KEY_SECTION, someOtherKeyConfig, CURRENT_CONFIG_LABEL, false);

    GitHubOAuthConfig objectUnderTest = objectUnderTest();

    assertEquals(objectUnderTest.getKeyConfig(currentKeyConfig).getKeyId(), currentKeyConfig);
    assertEquals(objectUnderTest.getKeyConfig(someOtherKeyConfig).getKeyId(), someOtherKeyConfig);
  }

  @Test
  public void shouldThrowWhenNoKeyConfigIsSetAsCurrent() {
    config.setBoolean(CONF_KEY_SECTION, "someKeyConfig", CURRENT_CONFIG_LABEL, false);

    assertThrows(IllegalStateException.class, this::objectUnderTest);
  }

  @Test
  public void shouldThrowWhenKeyConfigContainsDelimiterCharacter() {
    String invalidSubsection = "foo" + KEY_DELIMITER + "bar";
    config.setBoolean(CONF_KEY_SECTION, invalidSubsection, CURRENT_CONFIG_LABEL, false);

    IllegalStateException illegalStateException =
        assertThrows(IllegalStateException.class, this::objectUnderTest);

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

    assertThrows(IllegalStateException.class, this::objectUnderTest);
  }

  private GitHubOAuthConfig objectUnderTest() {
    return new GitHubOAuthConfig(config, canonicalWebUrl);
  }
}
