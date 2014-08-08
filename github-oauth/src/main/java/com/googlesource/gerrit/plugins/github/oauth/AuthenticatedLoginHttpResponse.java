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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class AuthenticatedLoginHttpResponse extends HttpServletResponseWrapper {
  private Cookie gerritCookie;
  private int status;
  private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
  private String characterEncoding = "UTF-8";
  private String contentType = "text/plain";

  public AuthenticatedLoginHttpResponse(HttpServletResponse httpResponse) {
    super(httpResponse);
  }

  @Override
  public void addCookie(Cookie cookie) {
    if(cookie.getName().equals(OAuthWebFilter.GERRIT_COOKIE_NAME)) {
      this.gerritCookie = cookie;
    }
  }

  public Cookie getGerritCookie() {
    return gerritCookie;
  }

  @Override
  public void addDateHeader(String name, long date) {
  }
  
  @Override
  public void addHeader(String name, String value) {
  }

  @Override
  public void addIntHeader(String name, int value) {
  }

  @Override
  public boolean containsHeader(String name) {
    return false;
  }

  @Override
  public String getHeader(String name) {
    return null;
  }

  @Override
  public Collection<String> getHeaderNames() {
    return Collections.emptyList();
  }

  @Override
  public Collection<String> getHeaders(String name) {
    return Collections.emptyList();
  }

  @Override
  public int getStatus() {
    return status;
  }

  @Override
  public void sendError(int sc, String msg) throws IOException {
    this.status = sc;
  }

  @Override
  public void sendError(int sc) throws IOException {
    this.status = sc;
  }

  @Override
  public void sendRedirect(String location) throws IOException {
    this.status = SC_MOVED_TEMPORARILY;
  }

  @Override
  public void setDateHeader(String name, long date) {
  }

  @Override
  public void setHeader(String name, String value) {
  }

  @Override
  public void setIntHeader(String name, int value) {
  }

  @Override
  public void setStatus(int sc, String sm) {
    this.status = sc;
  }

  @Override
  public void setStatus(int sc) {
    this.status = sc;
  }

  @Override
  public void flushBuffer() throws IOException {
  }

  @Override
  public int getBufferSize() {
    return 256;
  }

  @Override
  public String getCharacterEncoding() {
    return characterEncoding;
  }

  @Override
  public String getContentType() {
    return contentType;
  }

  @Override
  public Locale getLocale() {
    return super.getLocale();
  }

  @Override
  public ServletOutputStream getOutputStream() throws IOException {
    return new ServletOutputStream() {
      
      @Override
      public void write(int b) throws IOException {
        outputStream.write(b);
      }
      
      @Override
      public void setWriteListener(WriteListener arg0) {
      }
      
      @Override
      public boolean isReady() {
        return true;
      }
    };
  }

  @Override
  public ServletResponse getResponse() {
    return super.getResponse();
  }

  @Override
  public PrintWriter getWriter() throws IOException {
    return new PrintWriter(new OutputStreamWriter(outputStream,
        characterEncoding));
  }

  @Override
  public boolean isCommitted() {
    return false;
  }

  @Override
  public boolean isWrapperFor(Class<?> wrappedType) {
    return false;
  }

  @Override
  public boolean isWrapperFor(ServletResponse wrapped) {
    return false;
  }

  @Override
  public void reset() {
  }

  @Override
  public void resetBuffer() {
  }

  @Override
  public void setBufferSize(int size) {
  }

  @Override
  public void setCharacterEncoding(String charset) {
    this.characterEncoding = charset;
  }

  @Override
  public void setContentLength(int len) {
  }

  @Override
  public void setContentLengthLong(long length) {
  }

  @Override
  public void setContentType(String type) {
    this.contentType = type;
  }

  @Override
  public void setLocale(Locale loc) {
  }

  @Override
  public void setResponse(ServletResponse response) {
  }
}
