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
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class AES {
  private static final Logger log = LoggerFactory.getLogger(AES.class);

  private final SecretKeySpec secretKey;
  private final Cipher cipher;

  @Inject
  public AES(GitHubOAuthConfig config)
      throws IOException, NoSuchPaddingException, NoSuchAlgorithmException {
    secretKey = new SecretKeySpec(config.readPassword(), "AES");
    cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
  }

  public String encrypt(final String strToEncrypt) {
    try {
      cipher.init(Cipher.ENCRYPT_MODE, secretKey);
      return Base64.getEncoder()
          .encodeToString(cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      log.error("Could not encrypt secret", e);
    }
    return null;
  }

  public String decrypt(final String strToDecrypt) {
    try {
      cipher.init(Cipher.DECRYPT_MODE, secretKey);
      return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
    } catch (Exception e) {
      log.error("Could not decrypt secret", e);
    }
    return null;
  }
}
