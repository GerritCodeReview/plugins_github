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
package com.googlesource.gerrit.plugins.github.velocity;

import com.google.inject.Inject;
import com.google.inject.servlet.RequestScoped;
import org.apache.velocity.VelocityContext;

@RequestScoped
public class PluginVelocityModel {

  private final VelocityContext context;

  public VelocityContext getContext() {
    return context;
  }

  @Inject
  public PluginVelocityModel(VelocityContext context) {
    this.context = context;
  }

  public Object get(String key) {
    return context.get(key);
  }

  public Object[] getKeys() {
    return context.getKeys();
  }

  public Object put(String key, Object value) {
    return context.put(key, value);
  }

  public Object remove(String key) {
    return context.remove(key);
  }
}
