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

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.events.ConfigChangedListener;
import org.eclipse.jgit.events.ListenerHandle;
import org.eclipse.jgit.lib.Config;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class CompositeConfig extends Config {
  private final Config secureConfig;
  private final Config gerritConfig;

  @Inject
  public CompositeConfig(@GerritServerConfig Config config) {
    this.secureConfig = config;
    try {
      Field baseConfigField = Config.class.getDeclaredField("baseConfig");
      baseConfigField.setAccessible(true);
      this.gerritConfig = (Config) baseConfigField.get(config);
    } catch (SecurityException|NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
      throw new IllegalArgumentException("JGit baseConfig cannot be accessed from GerritServerConfig", e);
    }
  }

  public ListenerHandle addChangeListener(ConfigChangedListener listener) {
    return secureConfig.addChangeListener(listener);
  }

  public boolean equals(Object obj) {
    return secureConfig.equals(obj);
  }

  public int getInt(String section, String name, int defaultValue) {
    return secureConfig.getInt(section, name, defaultValue);
  }

  public int getInt(String section, String subsection, String name,
      int defaultValue) {
    return secureConfig.getInt(section, subsection, name, defaultValue);
  }

  public long getLong(String section, String name, long defaultValue) {
    return secureConfig.getLong(section, name, defaultValue);
  }

  public long getLong(String section, String subsection, String name,
      long defaultValue) {
    return secureConfig.getLong(section, subsection, name, defaultValue);
  }

  public boolean getBoolean(String section, String name, boolean defaultValue) {
    return secureConfig.getBoolean(section, name, defaultValue);
  }

  public boolean getBoolean(String section, String subsection, String name,
      boolean defaultValue) {
    return secureConfig.getBoolean(section, subsection, name, defaultValue);
  }

  public <T extends Enum<?>> T getEnum(String section, String subsection,
      String name, T defaultValue) {
    return secureConfig.getEnum(section, subsection, name, defaultValue);
  }

  public <T extends Enum<?>> T getEnum(T[] all, String section,
      String subsection, String name, T defaultValue) {
    return secureConfig.getEnum(all, section, subsection, name, defaultValue);
  }

  public Set<String> getNames(String section) {
    return getNames(section, null);
  }

  public Set<String> getNames(String section, String subsection) {
    Set<String> secureConfigNames = secureConfig.getNames(section, subsection);
    Set<String> gerritConfigNames = gerritConfig.getNames(section, subsection);
    return new ImmutableSet.Builder<String>()
        .addAll(secureConfigNames)
        .addAll(gerritConfigNames)
        .build();
  }

  public <T> T get(SectionParser<T> parser) {
    return secureConfig.get(parser);
  }

  public void fromText(String text) throws ConfigInvalidException {
    secureConfig.fromText(text);
  }

  public Set<String> getNames(String section, boolean recursive) {
    return secureConfig.getNames(section, recursive);
  }

  public Set<String> getNames(String section, String subsection,
      boolean recursive) {
    return secureConfig.getNames(section, subsection, recursive);
  }

  public String getString(String section, String subsection, String name) {
    return secureConfig.getString(section, subsection, name);
  }

  public String[] getStringList(String section, String subsection, String name) {
    return secureConfig.getStringList(section, subsection, name);
  }

  public Set<String> getSubsections(String section) {
    return secureConfig.getSubsections(section);
  }

  public Set<String> getSections() {
    return secureConfig.getSections();
  }

  public int hashCode() {
    return secureConfig.hashCode();
  }

  public void uncache(SectionParser<?> parser) {
    secureConfig.uncache(parser);
  }

  public void setInt(String section, String subsection, String name, int value) {
    secureConfig.setInt(section, subsection, name, value);
  }

  public void setLong(String section, String subsection, String name, long value) {
    secureConfig.setLong(section, subsection, name, value);
  }

  public void setBoolean(String section, String subsection, String name,
      boolean value) {
    secureConfig.setBoolean(section, subsection, name, value);
  }

  public <T extends Enum<?>> void setEnum(String section, String subsection,
      String name, T value) {
    secureConfig.setEnum(section, subsection, name, value);
  }

  public void setString(String section, String subsection, String name,
      String value) {
    secureConfig.setString(section, subsection, name, value);
  }

  public void unset(String section, String subsection, String name) {
    secureConfig.unset(section, subsection, name);
  }

  public void setStringList(String section, String subsection, String name,
      List<String> values) {
    secureConfig.setStringList(section, subsection, name, values);
  }

  public String toString() {
    return secureConfig.toString();
  }

  public void unsetSection(String section, String subsection) {
    secureConfig.unsetSection(section, subsection);
  }

  public String toText() {
    return secureConfig.toText();
  }
}
