package com.googlesource.gerrit.plugins.github.oauth;

import com.infradna.tool.bridge_method_injector.WithBridgeMethods;

public class FooClass {
  @WithBridgeMethods(
      value = {String.class, int.class},
      adapterMethod = "longToStringOrInt")
  public long getId() {
      return 42;
  }
}
