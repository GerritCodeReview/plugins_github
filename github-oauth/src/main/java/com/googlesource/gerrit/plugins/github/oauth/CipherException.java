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

import java.io.IOException;

/**
 * Signals that a cipher exception has occurred. This class can be used to represent exception for
 * both encryption and decryption failures
 */
public class CipherException extends IOException {
  private static final long serialVersionUID = 1L;

  /**
   * Constructs a {@code CipherException} with the specified detail message and cause
   *
   * @param message The detail message of the failure
   * @param cause The cause of the failure
   */
  public CipherException(String message, Exception cause) {
    super(message, cause);
  }
}
