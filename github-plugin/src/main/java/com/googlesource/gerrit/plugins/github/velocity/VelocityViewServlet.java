package com.googlesource.gerrit.plugins.github.velocity;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.googlesource.gerrit.plugins.github.oauth.GitHubLogin;

@Singleton
public class VelocityViewServlet extends HttpServlet {
  private static final Logger log = LoggerFactory.getLogger(VelocityViewServlet.class);
  private static final String STATIC_PREFIX = "/static";
  private static final long serialVersionUID = 529071287765413268L;
  private final RuntimeInstance velocityRuntime;
  private final Provider<PluginVelocityModel> modelProvider;

  @Inject
  public VelocityViewServlet(
      @Named("PluginRuntimeInstance") final RuntimeInstance velocityRuntime,
      Provider<PluginVelocityModel> modelProvider) {

    this.velocityRuntime = velocityRuntime;
    this.modelProvider = modelProvider;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    String pathInfo = req.getServletPath();
    String nextUrl = Objects.firstNonNull(req.getParameter("next"), "/");
    if (!pathInfo.startsWith(STATIC_PREFIX)) {
      resp.sendError(HttpStatus.SC_NOT_FOUND);
    }

    try {
      Template template =
            velocityRuntime.getTemplate(
                pathInfo, "UTF-8");
      VelocityContext context = modelProvider.get().getContext();
        context.put("nextUrl", nextUrl);
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

}
