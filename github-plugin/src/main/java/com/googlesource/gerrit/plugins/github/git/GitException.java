// Copyright (C) 2013 The Android Open Source Project
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
package com.googlesource.gerrit.plugins.github.git;

import java.io.IOException;

public abstract class GitException extends IOException {
  private static final long serialVersionUID = -1180349547385523064L;

  public GitException() {
    super();
  }

  public GitException(String message) {
    super(message);
  }

  public GitException(Throwable cause) {
    super(cause);
  }

  public GitException(String message, Throwable cause) {
    super(message, cause);
  }

  public abstract String getErrorDescription();
}
