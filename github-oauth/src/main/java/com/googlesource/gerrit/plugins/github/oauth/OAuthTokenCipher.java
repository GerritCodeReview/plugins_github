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

import com.google.common.base.Suppliers;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.function.Supplier;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

@Singleton
public class OAuthTokenCipher {
  private final GitHubOAuthConfig config;
  private final Supplier<Cipher> encryptCipher;
  private final Supplier<Cipher> decryptCipher;

  @Inject
  public OAuthTokenCipher(GitHubOAuthConfig config) throws IOException {
    this.config = config;
    SecretKeySpec secretKey =
        new SecretKeySpec(config.readPassword(), config.getSecretKeyAlgorithm());
    encryptCipher = Suppliers.memoize(() -> initCipher(Cipher.ENCRYPT_MODE, secretKey));
    decryptCipher = Suppliers.memoize(() -> initCipher(Cipher.DECRYPT_MODE, secretKey));
  }

  public String encrypt(final String strToEncrypt) throws CipherException {
    try {
      return Base64.getEncoder()
          .encodeToString(
              encryptCipher.get().doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8)));
    } catch (IllegalBlockSizeException | BadPaddingException e) {
      throw new CipherException("Could not encrypt oauth token", e);
    }
  }

  public String decrypt(final String strToDecrypt) throws CipherException {
    try {
      return new String(decryptCipher.get().doFinal(Base64.getDecoder().decode(strToDecrypt)));
    } catch (IllegalBlockSizeException | BadPaddingException e) {
      throw new CipherException("Could not decrypt oauth token", e);
    }
  }

  private Cipher initCipher(int mode, SecretKeySpec secretKey) {
    try {
      Cipher cipher = Cipher.getInstance(config.getCipherAlgorithm());
      cipher.init(mode, secretKey);
      return cipher;
    } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e) {
      throw new IllegalStateException("Could not get cipher", e);
    }
  }
}
