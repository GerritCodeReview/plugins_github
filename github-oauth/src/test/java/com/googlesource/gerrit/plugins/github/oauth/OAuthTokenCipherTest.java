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

import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.KeyConfig.CIPHER_ALGORITHM_DEFAULT;
import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.KeyConfig.SECRET_KEY_ALGORITHM_DEFAULT;
import static com.googlesource.gerrit.plugins.github.oauth.OAuthTokenCipher.dropKeyId;
import static com.googlesource.gerrit.plugins.github.oauth.OAuthTokenCipher.takeKeyId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OAuthTokenCipherTest {

  @Mock GitHubOAuthConfig gitHubOAuthConfig;
  @Mock GitHubOAuthConfig.KeyConfig version1KeyConfig;
  @Mock GitHubOAuthConfig.KeyConfig version2KeyConfig;

  private static final String VERSION1_KEY_ID = "version1";
  private static final String VERSION2_KEY_ID = "version2";

  @Before
  public void setUp() throws IOException {
    when(version1KeyConfig.getCipherAlgorithm()).thenReturn(CIPHER_ALGORITHM_DEFAULT);
    when(version1KeyConfig.getSecretKeyAlgorithm()).thenReturn(SECRET_KEY_ALGORITHM_DEFAULT);
    when(version1KeyConfig.getKeyId()).thenReturn(VERSION1_KEY_ID);
    when(version1KeyConfig.readPassword())
        .thenReturn("version1Password".getBytes(StandardCharsets.UTF_8));

    when(version2KeyConfig.getCipherAlgorithm()).thenReturn(CIPHER_ALGORITHM_DEFAULT);
    when(version2KeyConfig.getSecretKeyAlgorithm()).thenReturn(SECRET_KEY_ALGORITHM_DEFAULT);
    when(version2KeyConfig.getKeyId()).thenReturn(VERSION2_KEY_ID);
    when(version2KeyConfig.readPassword())
        .thenReturn("version2Password".getBytes(StandardCharsets.UTF_8));

    when(gitHubOAuthConfig.getKeyConfig(VERSION1_KEY_ID)).thenReturn(version1KeyConfig);
    when(gitHubOAuthConfig.getKeyConfig(VERSION2_KEY_ID)).thenReturn(version2KeyConfig);
  }

  @Test
  public void shouldEncryptAndDecryptAToken() throws IOException {
    when(gitHubOAuthConfig.getCurrentKeyConfig()).thenReturn(version1KeyConfig);

    String someOauthToken = "someToken";
    OAuthTokenCipher objectUnderTest = new OAuthTokenCipher(gitHubOAuthConfig);

    assertEquals(objectUnderTest.decrypt(objectUnderTest.encrypt(someOauthToken)), someOauthToken);
  }

  @Test
  public void shouldEncryptWithKeyId() throws IOException {
    when(gitHubOAuthConfig.getCurrentKeyConfig()).thenReturn(version1KeyConfig);

    String someOauthToken = "someToken";
    OAuthTokenCipher objectUnderTest = new OAuthTokenCipher(gitHubOAuthConfig);

    assertEquals(takeKeyId(objectUnderTest.encrypt(someOauthToken)), VERSION1_KEY_ID);
  }

  @Test
  public void shouldReturnAPrefixedBase64EncodedEncryptedString() throws IOException {
    when(gitHubOAuthConfig.getCurrentKeyConfig()).thenReturn(version1KeyConfig);

    String someOauthToken = "someToken";
    OAuthTokenCipher objectUnderTest = new OAuthTokenCipher(gitHubOAuthConfig);

    assertNotEquals(
        Base64.getDecoder().decode(dropKeyId(objectUnderTest.encrypt(someOauthToken))),
        someOauthToken.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  public void shouldStillBeAbleToDecryptATokenEncryptedWithANonCurrentKey() throws IOException {
    String someToken = "someToken";
    when(gitHubOAuthConfig.getCurrentKeyConfig()).thenReturn(version1KeyConfig);
    String encryptedWithVersion1 = new OAuthTokenCipher(gitHubOAuthConfig).encrypt(someToken);

    when(gitHubOAuthConfig.getCurrentKeyConfig()).thenReturn(version2KeyConfig);

    assertEquals(new OAuthTokenCipher(gitHubOAuthConfig).decrypt(encryptedWithVersion1), someToken);
  }

  @Test
  public void shouldPassThroughWhenDecryptingPlainTextStrings() throws IOException {
    String somePlainTextToken = "someToken";
    when(gitHubOAuthConfig.getCurrentKeyConfig()).thenReturn(version1KeyConfig);
    assertEquals(
        new OAuthTokenCipher(gitHubOAuthConfig).decrypt(somePlainTextToken), somePlainTextToken);
  }

  @Test
  public void shouldThrowWhenDecryptingATokenEncryptedANoLongerAvailableKey() {
    when(gitHubOAuthConfig.getCurrentKeyConfig()).thenReturn(version2KeyConfig);

    CipherException cipherException =
        assertThrows(
            CipherException.class,
            () -> new OAuthTokenCipher(gitHubOAuthConfig).decrypt("non-existing-key:foobar"));

    assertEquals(
        cipherException.getCause().getMessage(),
        "Could not find key-id 'non-existing-key' in configuration");
  }

  @Test
  public void shouldThrowWhenCipherAlgorithmIsNotValid() throws IOException {
    when(version1KeyConfig.getCipherAlgorithm()).thenReturn("Invalid cipher algorithm");
    when(gitHubOAuthConfig.getCurrentKeyConfig()).thenReturn(version1KeyConfig);

    OAuthTokenCipher objectUnderTest = new OAuthTokenCipher(gitHubOAuthConfig);

    assertThrows(CipherException.class, () -> objectUnderTest.encrypt("some token"));
  }

  @Test
  public void shouldThrowWhenKeyAlgorithmIsNotValid() throws IOException {
    when(version1KeyConfig.getSecretKeyAlgorithm()).thenReturn("Invalid Key algorithm");
    when(gitHubOAuthConfig.getCurrentKeyConfig()).thenReturn(version1KeyConfig);

    OAuthTokenCipher objectUnderTest = new OAuthTokenCipher(gitHubOAuthConfig);

    assertThrows(CipherException.class, () -> objectUnderTest.encrypt("some token"));
  }

  @Test
  public void shouldThrowWhenPasswordCouldNotBeRead() throws IOException {
    when(version1KeyConfig.readPassword()).thenThrow(new IOException("IO Exception"));
    when(gitHubOAuthConfig.getCurrentKeyConfig()).thenReturn(version1KeyConfig);

    assertThrows(IOException.class, () -> new OAuthTokenCipher(gitHubOAuthConfig));
  }

  @Test
  public void shouldThrowWhenDecryptingANonBase64String() throws IOException {
    when(gitHubOAuthConfig.getCurrentKeyConfig()).thenReturn(version1KeyConfig);

    OAuthTokenCipher objectUnderTest = new OAuthTokenCipher(gitHubOAuthConfig);

    assertThrows(
        IOException.class, () -> objectUnderTest.decrypt("current:some non-base64 string"));
  }
}
