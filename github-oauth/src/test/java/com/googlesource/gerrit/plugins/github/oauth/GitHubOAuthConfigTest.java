package com.googlesource.gerrit.plugins.github.oauth;

import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.CONF_KEY_SECTION;
import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.CONF_SECTION;
import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.KeyConfig.CIPHER_ALGORITHM_DEFAULT;
import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.KeyConfig.CIPHER_ALGO_CONFIG_LABEL;
import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.KeyConfig.CURRENT_CONFIG_LABEL;
import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.KeyConfig.KEY_ID_DEFAULT;
import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.KeyConfig.SECRET_KEY_ALGORITHM_DEFAULT;
import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.KeyConfig.SECRET_KEY_CONFIG_LABEL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.google.gerrit.extensions.client.AuthType;
import com.google.gerrit.httpd.CanonicalWebUrl;
import org.eclipse.jgit.lib.Config;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GitHubOAuthConfigTest {

  @Mock CanonicalWebUrl canonicalWebUrl;

  Config config;

  @Before
  public void setUp() {
    config = new Config();
    config.setString(CONF_SECTION, null, "clientSecret", "theSecret");
    config.setString(CONF_SECTION, null, "clientId", "theClientId");
    config.setString("auth", null, "httpHeader", "GITHUB_USER");
    config.setString("auth", null, "type", AuthType.HTTP.toString());
  }

  @Test
  public void shouldDefaultKeyConfigWhenNoSpecificConfigurationIsSet() {
    GitHubOAuthConfig objectUnderTest = new GitHubOAuthConfig(config, canonicalWebUrl);
    assertEquals(objectUnderTest.getCurrentKeyConfig().isCurrent(), true);
    assertEquals(
        objectUnderTest.getCurrentKeyConfig().getCipherAlgorithm(), CIPHER_ALGORITHM_DEFAULT);
    assertEquals(
        objectUnderTest.getCurrentKeyConfig().getSecretKeyAlgorithm(),
        SECRET_KEY_ALGORITHM_DEFAULT);
    assertEquals(objectUnderTest.getCurrentKeyConfig().getKeyId(), KEY_ID_DEFAULT);
  }

  @Test
  public void shouldReadASpecificKeyConfig() {
    String keySubsection = "someKeyConfig";
    String cipherAlgorithm = "AES/CFB8/NoPadding";
    String secretKeyAlgorithm = "DES";
    config.setBoolean(CONF_KEY_SECTION, keySubsection, CURRENT_CONFIG_LABEL, true);
    config.setString(CONF_KEY_SECTION, keySubsection, CIPHER_ALGO_CONFIG_LABEL, cipherAlgorithm);
    config.setString(CONF_KEY_SECTION, keySubsection, SECRET_KEY_CONFIG_LABEL, secretKeyAlgorithm);

    GitHubOAuthConfig objectUnderTest = new GitHubOAuthConfig(config, canonicalWebUrl);

    assertEquals(objectUnderTest.getCurrentKeyConfig().isCurrent(), true);
    assertEquals(objectUnderTest.getCurrentKeyConfig().getCipherAlgorithm(), cipherAlgorithm);
    assertEquals(objectUnderTest.getCurrentKeyConfig().getSecretKeyAlgorithm(), secretKeyAlgorithm);
    assertEquals(objectUnderTest.getCurrentKeyConfig().getKeyId(), keySubsection);
  }

  @Test
  public void shouldReturnTheExpectedKeyConfigAsCurrent() {
    config.setBoolean(CONF_KEY_SECTION, "currentKeyConfig", CURRENT_CONFIG_LABEL, true);
    config.setBoolean(CONF_KEY_SECTION, "someOtherKeyConfig", CURRENT_CONFIG_LABEL, false);

    GitHubOAuthConfig objectUnderTest = new GitHubOAuthConfig(config, canonicalWebUrl);

    assertEquals(objectUnderTest.getCurrentKeyConfig().getKeyId(), "currentKeyConfig");
  }

  @Test
  public void shouldReadMultipleKeyConfigs() {
    String currentKeyConfig = "currentKeyConfig";
    String someOtherKeyConfig = "someOtherKeyConfig";
    config.setBoolean(CONF_KEY_SECTION, currentKeyConfig, CURRENT_CONFIG_LABEL, true);
    config.setBoolean(CONF_KEY_SECTION, someOtherKeyConfig, CURRENT_CONFIG_LABEL, false);

    GitHubOAuthConfig objectUnderTest = new GitHubOAuthConfig(config, canonicalWebUrl);

    assertEquals(objectUnderTest.getKeyConfig(currentKeyConfig).getKeyId(), currentKeyConfig);
    assertEquals(objectUnderTest.getKeyConfig(someOtherKeyConfig).getKeyId(), someOtherKeyConfig);
  }

  @Test
  public void shouldThrowWhenNoKeyConfigIsSetAsCurrent() {
    config.setBoolean(CONF_KEY_SECTION, "someKeyConfig", CURRENT_CONFIG_LABEL, false);

    assertThrows(IllegalStateException.class, () -> new GitHubOAuthConfig(config, canonicalWebUrl));
  }

  @Test
  public void shouldThrowWhenMoreThanOneKeyConfigIsSetAsCurrent() {
    config.setBoolean(CONF_KEY_SECTION, "someKeyConfig", CURRENT_CONFIG_LABEL, true);
    config.setBoolean(CONF_KEY_SECTION, "someOtherKeyConfig", CURRENT_CONFIG_LABEL, true);

    assertThrows(IllegalStateException.class, () -> new GitHubOAuthConfig(config, canonicalWebUrl));
  }
}
