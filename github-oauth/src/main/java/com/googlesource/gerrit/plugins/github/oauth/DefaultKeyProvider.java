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
  private static Logger logger = LoggerFactory.getLogger(GitHubOAuthConfig.class);
  static final String DEFAULT_KEY_FILE = "default.key";

  private final Supplier<String> defaultConfig;

  @Inject
  DefaultKeyProvider(@PluginData File pluginData) {
    this.defaultConfig = Suppliers.memoize(() -> getDefaultConfig(pluginData));
  }

  /**
   * Throws {@link IllegalStateException} when default key files doesn't exist and cannot be
   * generated
   *
   * @return path to {@link DefaultKeyProvider#DEFAULT_KEY_FILE} file
   */
  @Override
  public String get() {
    return defaultConfig.get();
  }

  private String getDefaultConfig(File pluginData) {
    Path defaultKeyPath = pluginData.toPath().resolve(DEFAULT_KEY_FILE);
    File defaultKey = defaultKeyPath.toFile();
    if (defaultKey.isFile() && defaultKey.canRead() && defaultKey.length() > 0L) {
      return defaultKeyPath.toString();
    }

    byte[] token = generateToken(PASSWORD_LENGTH_DEFAULT);
    try {
      Files.write(defaultKeyPath, token);
      logger.info("Default was stored in {} file", defaultKeyPath);
      return defaultKeyPath.toString();
    } catch (IOException e) {
      logger.error("Storing default key under {} failed.", defaultKeyPath, e);
      throw new IllegalStateException("Generation of the default key file has failed", e);
    }
  }

  private byte[] generateToken(int length) {
    SecureRandom random = new SecureRandom();
    byte[] token = new byte[length];
    random.nextBytes(token);
    return token;
  }
}
