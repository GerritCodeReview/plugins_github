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

import com.google.common.base.MoreObjects;
import com.google.gerrit.server.IdentifiedUser;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.googlesource.gerrit.plugins.github.GitHubConfig.NextPage;
import com.googlesource.gerrit.plugins.github.oauth.GitHubLogin;
import com.googlesource.gerrit.plugins.github.oauth.ScopedProvider;

import org.apache.http.HttpStatus;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Singleton
public class VelocityViewServlet extends HttpServlet {
  private static final Logger log = LoggerFactory
      .getLogger(VelocityViewServlet.class);
  private static final String STATIC_PREFIX = "/static";
  private static final long serialVersionUID = 529071287765413268L;
  private final RuntimeInstance velocityRuntime;
  private final Provider<PluginVelocityModel> modelProvider;
  private final ScopedProvider<GitHubLogin> loginProvider;
  private final Provider<IdentifiedUser> userProvider;

  @Inject
  public VelocityViewServlet(
      @Named("PluginRuntimeInstance") final RuntimeInstance velocityRuntime,
      Provider<PluginVelocityModel> modelProvider,
      ScopedProvider<GitHubLogin> loginProvider, Provider<IdentifiedUser> userProvider) {

    this.velocityRuntime = velocityRuntime;
    this.modelProvider = modelProvider;
    this.loginProvider = loginProvider;
    this.userProvider = userProvider;
  }


  @Override
  public void service(ServletRequest request, ServletResponse response)
      throws ServletException, IOException {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse resp = (HttpServletResponse) response;

    String servletPath = req.getPathInfo();
    NextPage nextPage = (NextPage) req.getAttribute("destUrl");
    String destUrl = null;
    if (nextPage != null && !nextPage.uri.startsWith("/")) {
      destUrl =
          servletPath.substring(0, servletPath.lastIndexOf("/")) + "/"
              + nextPage.uri;
    }

    String pathInfo = MoreObjects.firstNonNull(destUrl, servletPath);
    if (!pathInfo.startsWith(STATIC_PREFIX)) {
      resp.sendError(HttpStatus.SC_NOT_FOUND);
    }

    try {
      Template template = velocityRuntime.getTemplate(pathInfo, "UTF-8");
      VelocityContext context = initVelocityModel(req).getContext();
      context.put("request", req);
      resp.setHeader("content-type", "text/html");
      resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
      template.merge(context, resp.getWriter());
    } catch (ResourceNotFoundException e) {
      log.error("Cannot load velocity template " + pathInfo, e);
      resp.sendError(HttpStatus.SC_NOT_FOUND);
    } catch (Exception e) {
      log.error("Error executing velocity template " + pathInfo, e);
      resp.sendError(HttpStatus.SC_INTERNAL_SERVER_ERROR,
          e.getLocalizedMessage());
    }
  }

  private PluginVelocityModel initVelocityModel(HttpServletRequest request) {
    PluginVelocityModel model = modelProvider.get();
    GitHubLogin gitHubLogin = loginProvider.get(request);
    model.put("myself", gitHubLogin.getMyself());
    model.put("user", userProvider.get());
    model.put("hub", gitHubLogin.getHub());

    for (Entry<String, String[]> reqPar : request.getParameterMap().entrySet()) {
      model.put(reqPar.getKey(), reqPar.getValue());
    }
    return model;
  }
}
