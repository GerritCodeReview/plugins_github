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

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/** Provides the ability to encrypt and decrypt an OAuth token */
@Singleton
public class OAuthTokenCipher {
  private final String currentCipherAlgorithm;
  private final SecretKeySpec currentSecretKey;
  private final String currentKeyPrefix;
  private final GitHubOAuthConfig config;

  /**
   * Constructs a {@code OAuthTokenCipher} using cipher algorithm specified in configuration
   *
   * @param config the github oauth configuration object
   * @throws IOException when the cipher could not be constructed
   */
  @Inject
  public OAuthTokenCipher(GitHubOAuthConfig config) throws IOException {
    GitHubOAuthConfig.KeyConfig currentKeyConfig = config.getCurrentKeyConfig();
    currentKeyPrefix = currentKeyConfig.getPrefix();
    currentCipherAlgorithm = currentKeyConfig.getCipherAlgorithm();
    currentSecretKey =
        new SecretKeySpec(
            currentKeyConfig.readPassword(), currentKeyConfig.getSecretKeyAlgorithm());
    this.config = config;
  }

  /**
   * Encrypts the provided string and returns its base64 representation, prefixed with the name of
   * the configuration subsection used to encrypt it, separated by ':'.
   *
   * <p>For example:
   *
   * <p>current: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
   *
   * @param plainText the string to encrypt
   * @return the base64-encoded encrypted string
   * @throws CipherException when the string could not be encrypted
   */
  public String encrypt(final String plainText) throws CipherException {
    try {
      return prefixWithCurrentKey(
          Base64.getEncoder()
              .encodeToString(
                  initCurrentCipherForEncryption()
                      .doFinal(plainText.getBytes(StandardCharsets.UTF_8))));
    } catch (IllegalBlockSizeException | BadPaddingException e) {
      throw new CipherException("Could not encrypt oauth token", e);
    }
  }

  /**
   * Decrypts the provided base64-encoded encrypted string, prefixed with the name of the
   * configuration subsection used to encrypt it, separated by ':'.
   *
   * <p>For example:
   *
   * <p>current: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
   *
   * @param base64EncryptedString the string to decrypt
   * @return the plainText string
   * @throws CipherException when the string could not be decrypted
   */
  public String decrypt(final String base64EncryptedString) throws CipherException {
    try {

      Cipher decryptCipher = getCipherFor(base64EncryptedString);
      return new String(
          decryptCipher.doFinal(Base64.getDecoder().decode(dropPrefix(base64EncryptedString))),
          StandardCharsets.UTF_8);
    } catch (IllegalStateException
        | IllegalArgumentException
        | IllegalBlockSizeException
        | BadPaddingException
        | IOException e) {
      throw new CipherException("Could not decrypt oauth token", e);
    }
  }

  private static Cipher initCipher(String cipherAlgorithm, SecretKeySpec secretKey, int mode)
      throws CipherException {
    try {
      Cipher cipher = Cipher.getInstance(cipherAlgorithm);
      cipher.init(mode, secretKey);
      return cipher;
    } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
      throw new CipherException("Could not init cipher", e);
    }
  }

  private Cipher initCurrentCipherForEncryption() throws CipherException {
    return initCipher(currentCipherAlgorithm, currentSecretKey, Cipher.ENCRYPT_MODE);
  }

  private String prefixWithCurrentKey(String base64EncodedString) {
    return String.format("%s:%s", currentKeyPrefix, base64EncodedString);
  }

  private static String extractFromString(String base64EncryptedString, int idx) {
    String[] tokens = base64EncryptedString.split(":");
    int nOfTokens = tokens.length;
    if (nOfTokens != 2) {
      throw new IllegalStateException(
          String.format(
              "The encrypted key is expected to contain 2 tokens (prefix:key), whereas it contains %d tokens",
              nOfTokens));
    }
    return tokens[idx];
  }

  @VisibleForTesting
  static String takePrefix(String base64EncryptedString) {
    return extractFromString(base64EncryptedString, 0);
  }

  @VisibleForTesting
  static String dropPrefix(String base64EncryptedString) {
    return extractFromString(base64EncryptedString, 1);
  }

  private Cipher getCipherFor(String base64EncryptedString) throws IOException {
    String prefix = takePrefix(base64EncryptedString);
    GitHubOAuthConfig.KeyConfig keyConfig = config.getKeyConfig(prefix);
    if (keyConfig == null) {
      throw new IllegalStateException(
          String.format("Could not find prefix '%s' in configuration", prefix));
    }
    return initCipher(
        keyConfig.getCipherAlgorithm(),
        new SecretKeySpec(keyConfig.readPassword(), keyConfig.getSecretKeyAlgorithm()),
        Cipher.DECRYPT_MODE);
  }
}
