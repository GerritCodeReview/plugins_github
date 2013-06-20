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
package com.googlesource.gerrit.plugins.github.oauth;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import com.google.common.base.Objects;

public class AuthenticatedHttpRequest extends HttpServletRequestWrapper {
  private String httpHeaderName;
  private String httpHeaderValue;
  private String targetUri;

  public AuthenticatedHttpRequest(HttpServletRequest request, String targetUri,
      String authHeaderName, String authHeaderValue) {
    super(request);
    this.httpHeaderName = authHeaderName;
    this.httpHeaderValue = authHeaderValue;
    this.targetUri = targetUri;
  }

  @Override
  public String getRequestURI() {
    return Objects.firstNonNull(targetUri, super.getRequestURI());
  }

  @Override
  public StringBuffer getRequestURL() {
    if (targetUri == null) {
      return super.getRequestURL();
    } else {
      return new StringBuffer(super.getRequestURL().toString()
          .replaceAll(super.getRequestURI(), targetUri));
    }
  }

  @Override
  public Enumeration<String> getHeaderNames() {

    final Enumeration<String> wrappedHeaderNames = super.getHeaderNames();
    return new Enumeration<String>() {

      boolean lastElement;
      boolean headerFound;

      @Override
      public boolean hasMoreElements() {
        if (wrappedHeaderNames.hasMoreElements()) {
          return true;
        } else if (!lastElement && !headerFound) {
          return true;
        } else {
          return false;
        }
      }

      @Override
      public String nextElement() {
        if (wrappedHeaderNames.hasMoreElements()) {
          String nextHeader = wrappedHeaderNames.nextElement();
          if (nextHeader.equalsIgnoreCase(httpHeaderName)) {
            headerFound = true;
          }
          return nextHeader;
        } else if (!lastElement && !headerFound) {
          lastElement = true;
          return httpHeaderName;
        } else {
          return null;
        }
      }

    };
  }

  @Override
  public String getHeader(String name) {
    if (name.equalsIgnoreCase(httpHeaderName)) {
      return httpHeaderValue;
    } else {
      return super.getHeader(name);
    }
  }
}
