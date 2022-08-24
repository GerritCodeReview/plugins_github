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
import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.KeyConfig.PASSWORD_DEVICE_CONFIG_LABEL;
import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.KeyConfig.SECRET_KEY_CONFIG_LABEL;
import static com.googlesource.gerrit.plugins.github.oauth.OAuthTokenCipher.splitKeyIdFromMaterial;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

import com.google.common.annotations.VisibleForTesting;
import com.google.gerrit.extensions.client.AuthType;
import com.google.gerrit.httpd.CanonicalWebUrl;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.eclipse.jgit.lib.Config;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OAuthTokenCipherTest {

  @Mock CanonicalWebUrl canonicalWebUrl;

  Config config;

  private static final String VERSION1_KEY_ID = "version1";
  private static final String VERSION2_KEY_ID = "version2";

  @Before
  public void setUp() {
    config = new Config();

    config.setString(CONF_SECTION, null, "clientSecret", "theSecret");
    config.setString(CONF_SECTION, null, "clientId", "theClientId");
    config.setString("auth", null, "httpHeader", "GITHUB_USER");
    config.setString("auth", null, "type", AuthType.HTTP.toString());

    config.setBoolean(CONF_KEY_SECTION, VERSION1_KEY_ID, CURRENT_CONFIG_LABEL, true);
    config.setBoolean(CONF_KEY_SECTION, VERSION2_KEY_ID, CURRENT_CONFIG_LABEL, false);
  }

  @Test
  public void shouldEncryptAndDecryptAToken() throws IOException {
    String someOauthToken = "someToken";

    OAuthTokenCipher objectUnderTest = objectUnderTest();

    assertEquals(objectUnderTest.decrypt(objectUnderTest.encrypt(someOauthToken)), someOauthToken);
  }

  @Test
  public void shouldEncryptWithKeyId() throws IOException {
    assertEquals(takeKeyId(objectUnderTest().encrypt("someToken")), VERSION1_KEY_ID);
  }

  @Test
  public void shouldReturnAPrefixedBase64EncodedEncryptedString() throws IOException {
    String someOauthToken = "someToken";

    assertNotEquals(
        Base64.getDecoder().decode(dropKeyId(objectUnderTest().encrypt(someOauthToken))),
        someOauthToken.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  public void shouldStillBeAbleToDecryptATokenEncryptedWithANonCurrentKey() throws IOException {
    String someToken = "someToken";
    String encryptedWithV1 = objectUnderTest().encrypt(someToken);

    config.setBoolean(CONF_KEY_SECTION, VERSION1_KEY_ID, CURRENT_CONFIG_LABEL, false);
    config.setBoolean(CONF_KEY_SECTION, VERSION2_KEY_ID, CURRENT_CONFIG_LABEL, true);

    assertEquals(objectUnderTest().decrypt(encryptedWithV1), someToken);
  }

  @Test
  public void shouldPassThroughWhenDecryptingPlainTextStrings() throws IOException {
    String somePlainTextToken = "someToken";
    assertEquals(objectUnderTest().decrypt(somePlainTextToken), somePlainTextToken);
  }

  @Test
  public void shouldThrowWhenDecryptingATokenEncryptedANoLongerAvailableKey() {
    CipherException cipherException =
        assertThrows(
            CipherException.class, () -> objectUnderTest().decrypt("non-existing-key:foobar"));

    assertEquals(
        cipherException.getCause().getMessage(),
        "Could not find key-id 'non-existing-key' in configuration");
  }

  @Test
  public void shouldThrowWhenCipherAlgorithmIsNotValid() {
    config.setString(
        CONF_KEY_SECTION, VERSION1_KEY_ID, CIPHER_ALGO_CONFIG_LABEL, "Invalid cipher algorithm");

    assertThrows(CipherException.class, () -> objectUnderTest().encrypt("some token"));
  }

  @Test
  public void shouldThrowWhenKeyAlgorithmIsNotValid() {
    config.setString(
        CONF_KEY_SECTION, VERSION1_KEY_ID, SECRET_KEY_CONFIG_LABEL, "Invalid Key algorithm");

    assertThrows(CipherException.class, () -> objectUnderTest().encrypt("some token"));
  }

  @Test
  public void shouldThrowWhenPasswordCouldNotBeRead() {
    config.setString(
        CONF_KEY_SECTION, VERSION1_KEY_ID, PASSWORD_DEVICE_CONFIG_LABEL, "/some/unexisting/file");

    assertThrows(IOException.class, this::objectUnderTest);
  }

  @Test
  public void shouldThrowWhenDecryptingANonBase64String() {
    assertThrows(
        IOException.class, () -> objectUnderTest().decrypt("current:some non-base64 string"));
  }

  @VisibleForTesting
  static String takeKeyId(String base64EncryptedString) {
    return splitKeyIdFromMaterial(base64EncryptedString).get(0);
  }

  @VisibleForTesting
  static String dropKeyId(String base64EncryptedString) {
    return splitKeyIdFromMaterial(base64EncryptedString).get(1);
  }

  private OAuthTokenCipher objectUnderTest() throws IOException {
    return new OAuthTokenCipher(new GitHubOAuthConfig(config, canonicalWebUrl));
  }
}
