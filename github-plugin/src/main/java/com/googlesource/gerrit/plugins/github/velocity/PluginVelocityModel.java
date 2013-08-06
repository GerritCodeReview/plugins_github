package com.googlesource.gerrit.plugins.github.velocity;

import org.apache.velocity.VelocityContext;

import com.google.inject.Inject;
import com.google.inject.servlet.RequestScoped;

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

  public Object remove(Object key) {
    return context.remove(key);
  }
}
