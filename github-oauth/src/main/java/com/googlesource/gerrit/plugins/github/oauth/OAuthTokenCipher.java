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
  private final String cipherAlgorithm;
  private final SecretKeySpec secretKey;

  /**
   * Constructs a {@code OAuthTokenCipher} using cipher algorithm specified in configuration
   *
   * @param config the github oauth configuration object
   * @throws IOException when the cipher could not be constructed
   */
  @Inject
  public OAuthTokenCipher(GitHubOAuthConfig config) throws IOException {
    cipherAlgorithm = config.getCipherAlgorithm();
    secretKey = new SecretKeySpec(config.readPassword(), config.getSecretKeyAlgorithm());
  }

  /**
   * Encrypts the provided string and returns its base64 representation
   *
   * @param plainText the string to encrypt
   * @return the base64-encoded encrypted string
   * @throws CipherException when the string could not be encrypted
   */
  public String encrypt(final String plainText) throws CipherException {
    try {
      Cipher encryptCipher = initCipher(Cipher.ENCRYPT_MODE);
      return Base64.getEncoder()
          .encodeToString(encryptCipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8)));
    } catch (IllegalBlockSizeException | BadPaddingException e) {
      throw new CipherException("Could not encrypt oauth token", e);
    }
  }

  /**
   * Decrypts the provided base64-encoded encrypted string
   *
   * @param base64EncryptedString the string to decrypt
   * @return the plainText string
   * @throws CipherException when the string could not be decrypted
   */
  public String decrypt(final String base64EncryptedString) throws CipherException {
    try {
      // lookup (or current true)
      Cipher decryptCipher = initCipher(Cipher.DECRYPT_MODE);
      return new String(
          decryptCipher.doFinal(Base64.getDecoder().decode(base64EncryptedString)),
          StandardCharsets.UTF_8);
    } catch (IllegalArgumentException | IllegalBlockSizeException | BadPaddingException e) {
      throw new CipherException("Could not decrypt oauth token", e);
    }
  }

  private Cipher initCipher(int mode) throws CipherException {
    try {
      Cipher cipher = Cipher.getInstance(cipherAlgorithm);
      cipher.init(mode, secretKey);
      return cipher;
    } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
      throw new CipherException("Could not init cipher", e);
    }
  }
}
