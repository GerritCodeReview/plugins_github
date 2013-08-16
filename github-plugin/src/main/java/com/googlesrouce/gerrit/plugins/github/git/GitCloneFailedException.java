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
package com.googlesrouce.gerrit.plugins.github.git;

import org.eclipse.jgit.api.errors.JGitInternalException;

public class GitCloneFailedException extends GitException {
  public GitCloneFailedException(String remoteUrl, Throwable e) {
    super("Failed to clone from repository " + remoteUrl, e);
  }

  private static final long serialVersionUID = 1619949108894445899L;

  @Override
  public String getErrorDescription() {
    return "Clone failed" + getCauseDescription(getCause());
  }

  private String getCauseDescription(Throwable cause) {
    if (cause == null) {
      return "";
    } else if (JGitInternalException.class.isAssignableFrom(cause.getClass())) {
      Throwable innerCause = cause.getCause();
      return innerCause != null ? getCauseDescription(cause.getCause())
          : "JGit internal error";
    } else {
      return getDecamelisedName(cause);
    }
  }

  private String getDecamelisedName(Throwable cause) {
    StringBuilder name = new StringBuilder();
    String causeName = cause.getClass().getName();
    causeName = causeName.substring(causeName.lastIndexOf('.')+1);
    for (char causeChar : causeName.toCharArray()) {
      if(Character.isUpperCase(causeChar)) {
        name.append(" ");
        name.append(Character.toLowerCase(causeChar));
      } else {
        name.append(causeChar);
      }
    }
    
    return name.toString();
  }

}
