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
package com.googlesource.gerrit.plugins.github.wizard;

import com.google.gerrit.server.IdentifiedUser;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ControllerErrors {
  private static final Logger log = LoggerFactory
      .getLogger(ControllerErrors.class);

  private final IdentifiedUser user;

  @Inject
  public ControllerErrors(final IdentifiedUser user) {
    this.user = user;
  }

  public void submit(Exception e) {
    log.error(String.format("User:%s Controller:%s Exception:%s '%s'",
        getUser(), getController(), e.getClass(), e.getLocalizedMessage()), e);
  }

  private String getController() {
    StackTraceElement[] stack = Thread.currentThread().getStackTrace();
    StackTraceElement caller = stack[stack.length - 1];
    return caller.getClassName() + "." + caller.getMethodName();
  }

  private Object getUser() {
    return user.getUserName() + " '" + user.getNameEmail() + "'";
  }
}
