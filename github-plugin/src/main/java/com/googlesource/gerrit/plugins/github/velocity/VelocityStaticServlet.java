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
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.httpd.raw.SiteStaticDirectoryServlet;
import com.google.gerrit.util.http.CacheHeaders;
import com.google.gerrit.util.http.RequestUtil;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.velocity.runtime.RuntimeInstance;
import org.apache.velocity.runtime.resource.Resource;

/** Sends static content using Velocity resource resolver */
@SuppressWarnings("serial")
@Singleton
public class VelocityStaticServlet extends HttpServlet {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();
  private static final Map<String, String> MIME_TYPES = Maps.newHashMap();
  private static final String STATIC_PATH_PREFIX = "static/";

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

  private static final Set<String> BINARY_TYPES =
      Set.of("pdf", "jpeg", "jpg", "gif", "png", "tiff", "tif");

  private static String contentType(final String name) {
    final int dot = name.lastIndexOf('.');
    final String ext = 0 < dot ? name.substring(dot + 1) : "";
    final String type = MIME_TYPES.get(ext);
    return type != null ? type : "application/octet-stream";
  }

  private static byte[] readResource(final Resource p) throws IOException {
    try (Reader in =
            p.getResourceLoader().getResourceReader(p.getName(), StandardCharsets.UTF_8.name());
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream()) {
      IOUtils.copy(in, byteOut);
      return byteOut.toByteArray();
    }
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
  private final SiteStaticDirectoryServlet siteStaticServlet;

  @Inject
  VelocityStaticServlet(
      @Named("PluginRuntimeInstance") final Provider<RuntimeInstance> velocityRuntimeProvider,
      SiteStaticDirectoryServlet siteStaticServlet) {
    this.velocity = velocityRuntimeProvider.get();
    this.siteStaticServlet = siteStaticServlet;
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
      log.atSevere().withCause(e).log("Cannot resolve resource " + resourceName);
      return null;
    }
  }

  private boolean binaryResource(String resourceName) {
    final int dot = resourceName.lastIndexOf('.');
    final String ext = 0 < dot ? resourceName.substring(dot + 1) : "";
    return BINARY_TYPES.contains(ext.toLowerCase());
  }

  private String resourceName(HttpServletRequest req) {
    final String name = req.getPathInfo();
    if (name.length() < 2 || !name.startsWith("/") || isUnreasonableName(name)) {
      // Too short to be a valid file name, or doesn't start with
      // the path info separator like we expected.
      //
      return null;
    }

    return name.substring(1);
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
  protected void doGet(final HttpServletRequest req, final HttpServletResponse rsp)
      throws IOException, ServletException {
    String resourceName = resourceName(req);
    if (binaryResource(resourceName) && resourceName.startsWith(STATIC_PATH_PREFIX)) {
      HttpServletRequestWrapper mappedReq =
          new HttpServletRequestWrapper(req) {

            @Override
            public String getPathInfo() {
              return super.getPathInfo().substring(STATIC_PATH_PREFIX.length());
            }
          };
      siteStaticServlet.service(mappedReq, rsp);
      return;
    }

    final Resource p = local(req);
    if (p == null) {
      CacheHeaders.setNotCacheable(rsp);
      rsp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    final String type = contentType(p.getName());
    final byte[] tosend;
    if (!type.equals("application/x-javascript") && RequestUtil.acceptsGzipEncoding(req)) {
      rsp.setHeader("Content-Encoding", "gzip");
      tosend = compress(readResource(p));
    } else {
      tosend = readResource(p);
    }

    CacheHeaders.setCacheable(req, rsp, 12, TimeUnit.HOURS);
    rsp.setDateHeader("Last-Modified", p.getLastModified());
    rsp.setContentType(type);
    rsp.setContentLength(tosend.length);
    try (OutputStream out = rsp.getOutputStream()) {
      out.write(tosend);
    }
  }
}
