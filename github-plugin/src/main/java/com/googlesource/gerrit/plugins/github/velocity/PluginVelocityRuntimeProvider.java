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

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.SitePaths;
import com.google.gerrit.server.mail.VelocityRuntimeProvider.Slf4jLogChute;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;
import com.google.inject.Singleton;

import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.RuntimeInstance;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.runtime.resource.loader.JarResourceLoader;

import java.io.File;
import java.util.Properties;

@Singleton
public class PluginVelocityRuntimeProvider implements Provider<RuntimeInstance> {
  private static final String VELOCITY_FILE_RESOURCE_LOADER_PATH =
      "file.resource.loader.path";
  private static final String VELOCITY_FILE_RESOURCE_LOADER_CLASS =
      "file.resource.loader.class";
  private static final String VELOCITY_CLASS_RESOURCE_LOADER_CLASS =
      "class.resource.loader.class";
  private static final String VELOCITY_JAR_RESOURCE_LOADER_CLASS =
      "jar.resource.loader.class";
  private static final String VELOCITY_JAR_RESOURCE_LOADER_PATH =
      "jar.resource.loader.path";
  private static final String VELOCITY_RESOURCE_LOADER = "resource.loader";
  private final SitePaths site;
  private String pluginName;

  @Inject
  PluginVelocityRuntimeProvider(SitePaths site, @PluginName String pluginName) {
    this.site = site;
    this.pluginName = pluginName;
  }

  public RuntimeInstance get() {
    String pkg = "org.apache.velocity.runtime.resource.loader";

    Properties p = new Properties();
    p.setProperty(RuntimeConstants.VM_PERM_INLINE_LOCAL, "true");
    p.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS,
        Slf4jLogChute.class.getName());
    p.setProperty(RuntimeConstants.RUNTIME_REFERENCES_STRICT, "true");
    p.setProperty("runtime.log.logsystem.log4j.category", "velocity");

    p.setProperty(VELOCITY_RESOURCE_LOADER, "file, class, jar");
    p.setProperty(VELOCITY_FILE_RESOURCE_LOADER_CLASS, pkg
        + ".FileResourceLoader");
    p.setProperty(VELOCITY_FILE_RESOURCE_LOADER_PATH,
        new File(site.static_dir.getAbsolutePath(), "..").getAbsolutePath());
    p.setProperty(VELOCITY_CLASS_RESOURCE_LOADER_CLASS,
        ClasspathResourceLoader.class.getName());
    p.setProperty(VELOCITY_JAR_RESOURCE_LOADER_CLASS,
        JarResourceLoader.class.getName());
    p.setProperty(VELOCITY_JAR_RESOURCE_LOADER_PATH, "jar:file:"
        + new File(site.plugins_dir, pluginName + ".jar").getAbsolutePath());

    RuntimeInstance ri = new RuntimeInstance();
    try {
      ri.init(p);
    } catch (Exception err) {
      throw new ProvisionException("Cannot configure Velocity templates", err);
    }
    return ri;
  }
}
