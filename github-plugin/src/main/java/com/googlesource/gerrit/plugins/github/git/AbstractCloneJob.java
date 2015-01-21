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

import com.google.inject.ProvisionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractCloneJob {
  private static final Logger LOG = LoggerFactory.getLogger(AbstractCloneJob.class);

  public AbstractCloneJob() {
    super();
  }

  protected String getErrorDescription(Throwable exception) {
    LOG.error("Job " + this + " FAILED", exception);
    if(GitException.class.isAssignableFrom(exception.getClass())) {
      return ((GitException) exception).getErrorDescription();
    } else if(ProvisionException.class.isAssignableFrom(exception.getClass())){
      Throwable cause = exception.getCause();
      if(cause != null) {
      return getErrorDescription(cause);
      } else {
        return "Import startup failed";
      }
    } else {
      return "Internal error";
    }
  }

}
