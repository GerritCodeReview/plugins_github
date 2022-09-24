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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Suppliers;
import com.google.gerrit.extensions.annotations.PluginData;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DefaultKeyProvider implements Provider<String> {
  private static final Logger logger = LoggerFactory.getLogger(GitHubOAuthConfig.class);
  @VisibleForTesting static final String DEFAULT_KEY_FILE = "default.key";

  private final Supplier<String> defaultConfig;

  @Inject
  DefaultKeyProvider(@PluginData Path pluginData) {
    this.defaultConfig = Suppliers.memoize(() -> getDefaultConfig(pluginData));
  }

  /**
   * Returns the path to {@link DefaultKeyProvider#DEFAULT_KEY_FILE} file that contains password.
   * Note that {@link DefaultKeyProvider#DEFAULT_KEY_FILE} file is created and its content generated
   * when it doesn't exist.
   *
   * @return path to {@link DefaultKeyProvider#DEFAULT_KEY_FILE} file
   * @throws {@link IllegalStateException} when ({@code default.key} file is a directory, cannot be
   *     read, has invalid length or doesn't exist and cannot be created
   */
  @Override
  public String get() {
    return defaultConfig.get();
  }

  private String getDefaultConfig(Path pluginData) {
    Path defaultKeyPath = pluginData.resolve(DEFAULT_KEY_FILE);
    File defaultKey = defaultKeyPath.toFile();

    if (defaultKey.isDirectory()) {
      logErrorAndThrow("The %s is directory and regular file is expected.", defaultKeyPath);
    }

    if (defaultKey.isFile()) {
      if (!defaultKey.canRead()) {
        logErrorAndThrow("The %s exists but cannot be read.", defaultKeyPath);
      }

      long length = defaultKey.length();
      if (length != PASSWORD_LENGTH_DEFAULT) {
        logErrorAndThrow(
            "The %s exists but has invalid length %d. The expected length is %d",
            defaultKeyPath, length, PASSWORD_LENGTH_DEFAULT);
      }

      return defaultKeyPath.toString();
    }

    byte[] token = generateToken();
    try {
      Files.write(defaultKeyPath, token);
      logger.info("Default was stored in {} file", defaultKeyPath);
      return defaultKeyPath.toString();
    } catch (IOException e) {
      logger.error("Storing default key under {} failed.", defaultKeyPath, e);
      throw new IllegalStateException("Generation of the default key file has failed", e);
    }
  }

  private byte[] generateToken() {
    SecureRandom random = new SecureRandom();
    byte[] token = new byte[PASSWORD_LENGTH_DEFAULT];
    random.nextBytes(token);
    return token;
  }

  private void logErrorAndThrow(String msg, Object... parameters) {
    String log = String.format(msg, parameters);
    logger.error(log);
    throw new IllegalStateException(log);
  }
}
