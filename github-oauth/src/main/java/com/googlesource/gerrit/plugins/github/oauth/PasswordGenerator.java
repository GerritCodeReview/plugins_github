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

import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.KeyConfig.PASSWORD_LENGTH_DEFAULT;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PasswordGenerator {
  private static final Logger logger = LoggerFactory.getLogger(PasswordGenerator.class);
  public static final String DEFAULT_PASSWORD_FILE = "default.key";

  /**
   * Generates default password and stores under given {@code Path}. Note that if password already
   * exists it is not regenerated.
   *
   * @param passwordFilePath path that should contain the default password; cannot be {@code null}
   * @throws {@link IllegalStateException} when file denoted by given {@code Path} is a directory,
   *     cannot be read, has invalid length or doesn't exist and cannot be created
   * @return {@code true} if password was generated, {@code false} if it already exists
   */
  public boolean generate(Path passwordFilePath) {
    requireNonNull(passwordFilePath);

    File passwordFile = passwordFilePath.toFile();

    if (passwordFile.isDirectory()) {
      throw logErrorAndCreateRuntimeException(
          "'%s' is directory whilst a regular file was expected.", passwordFilePath);
    }

    if (passwordFile.isFile()) {
      if (!passwordFile.canRead()) {
        throw logErrorAndCreateRuntimeException(
            "'%s' password file exists, but cannot be read.", passwordFilePath);
      }

      long length = passwordFile.length();
      if (length != PASSWORD_LENGTH_DEFAULT) {
        throw logErrorAndCreateRuntimeException(
            "'%s' password file exists but has an invalid length of %d bytes. The expected length"
                + " is %d bytes.",
            passwordFilePath, length, PASSWORD_LENGTH_DEFAULT);
      }
      return false;
    }

    byte[] token = generateToken();
    try {
      Files.write(passwordFilePath, token);
      logger.info("Password was stored in {} file", passwordFilePath);
      return true;
    } catch (IOException e) {
      throw logErrorAndCreateRuntimeException(e, "Password generation has failed");
    }
  }

  private byte[] generateToken() {
    SecureRandom random = new SecureRandom();
    byte[] token = new byte[PASSWORD_LENGTH_DEFAULT];
    random.nextBytes(token);
    return token;
  }

  private IllegalStateException logErrorAndCreateRuntimeException(
      String msg, Object... parameters) {
    return logErrorAndCreateRuntimeException(null, msg, parameters);
  }

  private IllegalStateException logErrorAndCreateRuntimeException(
      Exception e, String msg, Object... parameters) {
    String log = String.format(msg, parameters);
    if (e != null) {
      logger.error(log, e);
      return new IllegalStateException(log, e);
    }

    logger.error(log);
    return new IllegalStateException(log);
  }
}
