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

import com.google.common.collect.Maps;
import com.google.gwtexpui.server.CacheHeaders;
import com.google.gwtjsonrpc.server.RPCServletUtils;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.apache.commons.io.IOUtils;
import org.apache.velocity.runtime.RuntimeInstance;
import org.apache.velocity.runtime.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Sends static content using Velocity resource resolver */
@SuppressWarnings("serial")
@Singleton
public class VelocityStaticServlet extends HttpServlet {
  private static final Logger log = LoggerFactory
      .getLogger(VelocityStaticServlet.class);
  private static final Map<String, String> MIME_TYPES = Maps.newHashMap();
  static {
    MIME_TYPES.put("html", "text/html");
    MIME_TYPES.put("htm", "text/html");
    MIME_TYPES.put("js", "application/x-javascript");
    MIME_TYPES.put("css", "text/css");
    MIME_TYPES.put("rtf", "text/rtf");
    MIME_TYPES.put("txt", "text/plain");
    MIME_TYPES.put("text", "text/plain");
    MIME_TYPES.put("pdf", "application/pdf");
    MIME_TYPES.put("jpeg", "image/jpeg");
    MIME_TYPES.put("jpg", "image/jpeg");
    MIME_TYPES.put("gif", "image/gif");
    MIME_TYPES.put("png", "image/png");
    MIME_TYPES.put("tiff", "image/tiff");
    MIME_TYPES.put("tif", "image/tiff");
    MIME_TYPES.put("svg", "image/svg+xml");
  }

  private static String contentType(final String name) {
    final int dot = name.lastIndexOf('.');
    final String ext = 0 < dot ? name.substring(dot + 1) : "";
    final String type = MIME_TYPES.get(ext);
    return type != null ? type : "application/octet-stream";
  }

  private static byte[] readResource(final Resource p) throws IOException {
    final InputStream in = p.getResourceLoader().getResourceStream(p.getName());
    ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
    try {
      IOUtils.copy(in, byteOut);
    } finally {
      in.close();
      byteOut.close();
    }

    return byteOut.toByteArray();
  }

  private static byte[] compress(final byte[] raw) throws IOException {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final GZIPOutputStream gz = new GZIPOutputStream(out);
    gz.write(raw);
    gz.finish();
    gz.flush();
    return out.toByteArray();
  }

  private final RuntimeInstance velocity; 

  @Inject
  VelocityStaticServlet(
      @Named("PluginRuntimeInstance") final Provider<RuntimeInstance> velocityRuntimeProvider) {
    this.velocity = velocityRuntimeProvider.get();
  }

  private Resource local(final HttpServletRequest req) {
    final String name = req.getPathInfo();
    if (name.length() < 2 || !name.startsWith("/") || isUnreasonableName(name)) {
      // Too short to be a valid file name, or doesn't start with
      // the path info separator like we expected.
      //
      return null;
    }

    String resourceName = name.substring(1);
    try {
      return velocity.getContent(resourceName);
    } catch (Exception e) {
      log.error("Cannot resolve resource " + resourceName, e);
      return null;
    }
  }

  private static boolean isUnreasonableName(String name) {
    if (name.charAt(name.length() - 1) == '/') return true; // no suffix
    if (name.indexOf('\\') >= 0) return true; // no windows/dos stlye paths
    if (name.startsWith("../")) return true; // no "../etc/passwd"
    if (name.contains("/../")) return true; // no "foo/../etc/passwd"
    if (name.contains("/./")) return true; // "foo/./foo" is insane to ask
    if (name.contains("//")) return true; // windows UNC path can be "//..."

    return false; // is a reasonable name
  }

  @Override
  protected long getLastModified(final HttpServletRequest req) {
    final Resource p = local(req);
    return p != null ? p.getLastModified() : -1;
  }

  @Override
  protected void doGet(final HttpServletRequest req,
      final HttpServletResponse rsp) throws IOException {
    final Resource p = local(req);
    if (p == null) {
      CacheHeaders.setNotCacheable(rsp);
      rsp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    final String type = contentType(p.getName());
    final byte[] tosend;
    if (!type.equals("application/x-javascript")
        && RPCServletUtils.acceptsGzipEncoding(req)) {
      rsp.setHeader("Content-Encoding", "gzip");
      tosend = compress(readResource(p));
    } else {
      tosend = readResource(p);
    }

    CacheHeaders.setCacheable(req, rsp, 12, TimeUnit.HOURS);
    rsp.setDateHeader("Last-Modified", p.getLastModified());
    rsp.setContentType(type);
    rsp.setContentLength(tosend.length);
    final OutputStream out = rsp.getOutputStream();
    try {
      out.write(tosend);
    } finally {
      out.close();
    }
  }
}
