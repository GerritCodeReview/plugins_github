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

import static com.googlesource.gerrit.plugins.github.oauth.DefaultKeyProvider.DEFAULT_KEY_FILE;
import static com.googlesource.gerrit.plugins.github.oauth.GitHubOAuthConfig.KeyConfig.PASSWORD_LENGTH_DEFAULT;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DefaultKeyProviderTest {
  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void shouldGenerateKeyFileWithPasswordDefaultLength() throws IOException {
    Path defaultKeyPath = Path.of(objectUnderTest().get());
    assertTrue(Files.isRegularFile(defaultKeyPath));

    byte[] token = Files.readAllBytes(defaultKeyPath);
    assertEquals(
        String.format(
            "Generated password length doesn't equal to expected %d", PASSWORD_LENGTH_DEFAULT),
        token.length,
        PASSWORD_LENGTH_DEFAULT);
  }

  @Test
  public void shouldNotGenerateNewDefaultKeyForConsecutiveGet() throws IOException {
    DefaultKeyProvider objectUnderTest = objectUnderTest();

    byte[] expected = Files.readAllBytes(Path.of(objectUnderTest.get()));
    byte[] token = Files.readAllBytes(Path.of(objectUnderTest.get()));
    assertArrayEquals(
        "Consequtive call to get default key resulted in a new generation", expected, token);
  }

  @Test
  public void shouldNotGenerateNewDefaultKeyIfOneAlreadyExistAndIsNotEmpty() throws IOException {
    Path pluginData = temporaryFolder.newFolder().toPath();

    byte[] expected = Files.readAllBytes(Path.of(objectUnderTest(pluginData).get()));
    byte[] token = Files.readAllBytes(Path.of(objectUnderTest(pluginData).get()));
    assertArrayEquals("Existing default key was overwritten", expected, token);
  }

  @Test
  public void shouldGenerateDifferentContentForDifferentSites() throws IOException {
    byte[] siteA = Files.readAllBytes(Path.of(objectUnderTest().get()));
    byte[] siteB = Files.readAllBytes(Path.of(objectUnderTest().get()));
    assertFalse(
        "The same default key was generated for two different sites", Arrays.equals(siteA, siteB));
  }

  @Test
  public void shouldThrowIllegalStateExceptionWhenDefaultKeyIsDirectory() throws IOException {
    Path pluginData = temporaryFolder.newFolder().toPath();

    // create dir with DEFAULT_KEY_FILE name under plugin data dir
    assertTrue(pluginData.resolve(DEFAULT_KEY_FILE).toFile().mkdir());

    IllegalStateException illegalStateException =
        assertThrows(IllegalStateException.class, () -> objectUnderTest(pluginData).get());

    assertTrue(
        illegalStateException.getMessage().endsWith("is directory and regular file is expected."));
  }

  private DefaultKeyProvider objectUnderTest() throws IOException {
    return objectUnderTest(temporaryFolder.newFolder().toPath());
  }

  private DefaultKeyProvider objectUnderTest(Path pluginData) {
    return new DefaultKeyProvider(pluginData);
  }
}
