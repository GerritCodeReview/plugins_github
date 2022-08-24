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

import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.CIPHER_ALGORITHM_DEFAULT;
import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.SECRET_KEY_ALGORITHM_DEFAULT;
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

  @Before
  public void setUp() throws IOException {
    when(gitHubOAuthConfig.getCipherAlgorithm()).thenReturn(CIPHER_ALGORITHM_DEFAULT);
    when(gitHubOAuthConfig.getSecretKeyAlgorithm()).thenReturn(SECRET_KEY_ALGORITHM_DEFAULT);
    when(gitHubOAuthConfig.readPassword())
        .thenReturn("somePassword1234".getBytes(StandardCharsets.UTF_8));
  }

  @Test
  public void shouldEncryptAndDecryptAToken() throws IOException {
    String someOauthToken = "someToken";
    OAuthTokenCipher objectUnderTest = new OAuthTokenCipher(gitHubOAuthConfig);

    assertEquals(objectUnderTest.decrypt(objectUnderTest.encrypt(someOauthToken)), someOauthToken);
  }

  @Test
  public void shouldReturnABase64EncodedEncryptedString() throws IOException {
    String someOauthToken = "someToken";
    OAuthTokenCipher objectUnderTest = new OAuthTokenCipher(gitHubOAuthConfig);

    assertNotEquals(
        Base64.getDecoder().decode(objectUnderTest.encrypt(someOauthToken)),
        someOauthToken.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  public void shouldThrowWhenCipherAlgorithmIsNotValid() throws IOException {
    when(gitHubOAuthConfig.getCipherAlgorithm()).thenReturn("Invalid cipher algorithm");
    OAuthTokenCipher objectUnderTest = new OAuthTokenCipher(gitHubOAuthConfig);

    assertThrows(CipherException.class, () -> objectUnderTest.encrypt("some token"));
  }

  @Test
  public void shouldThrowWhenKeyAlgorithmIsNotValid() throws IOException {
    when(gitHubOAuthConfig.getSecretKeyAlgorithm()).thenReturn("Invalid Key algorithm");
    OAuthTokenCipher objectUnderTest = new OAuthTokenCipher(gitHubOAuthConfig);

    assertThrows(CipherException.class, () -> objectUnderTest.encrypt("some token"));
  }

  @Test
  public void shouldThrowWhenPasswordCouldNotBeRead() throws IOException {
    when(gitHubOAuthConfig.readPassword()).thenThrow(new IOException("IO Exception"));

    assertThrows(IOException.class, () -> new OAuthTokenCipher(gitHubOAuthConfig));
  }

  @Test
  public void shouldThrowWhenDecryptingANonBase64String() throws IOException {
    OAuthTokenCipher objectUnderTest = new OAuthTokenCipher(gitHubOAuthConfig);

    assertThrows(IOException.class, () -> objectUnderTest.decrypt("some non-base64 string"));
  }
}
