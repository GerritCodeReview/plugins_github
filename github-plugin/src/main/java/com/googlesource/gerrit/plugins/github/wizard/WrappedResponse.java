package com.googlesource.gerrit.plugins.github.wizard;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class WrappedResponse extends HttpServletResponseWrapper {

  public WrappedResponse(HttpServletResponse response) {
    super(response);
  }

}
