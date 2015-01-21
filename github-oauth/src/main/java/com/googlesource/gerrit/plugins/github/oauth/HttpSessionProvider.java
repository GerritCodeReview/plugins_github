// Copyright (C) 2014 The Android Open Source Project
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

import com.google.inject.Inject;
import com.google.inject.Provider;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public abstract class HttpSessionProvider<T> implements ScopedProvider<T> {

  @Inject
  private Provider<T> provider;

  @Inject
  private Provider<HttpServletRequest> httpRequestProvider;

  @Override
  public T get() {
    return get(httpRequestProvider.get());
  }

  @Override
  public T get(final HttpServletRequest req) {
    HttpSession session = req.getSession();
    String singletonKey = getClass().getName();

    synchronized (this) {
      @SuppressWarnings("unchecked")
      T instance = (T) session.getAttribute(singletonKey);
      if (instance == null) {
        instance = provider.get();
        session.setAttribute(singletonKey, instance);
      }
      return instance;
    }
  }

  @Override
  public HttpServletRequest getScopedRequest() {
    return httpRequestProvider.get();
  }
}
