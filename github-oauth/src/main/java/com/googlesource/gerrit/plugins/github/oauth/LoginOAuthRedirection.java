package com.googlesource.gerrit.plugins.github.oauth;

import com.google.gerrit.server.config.CanonicalWebUrl;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Optional;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class LoginOAuthRedirection implements Filter {
  private static final Logger logger = LoggerFactory.getLogger(LoginOAuthRedirection.class);
  static final String X_FORWARDED_HOST_HTTP_HEADER = "X-Forwarded-Host";
  private final URL canonicalUrl;

  @Inject
  LoginOAuthRedirection(@CanonicalWebUrl String canonicalUrl) throws MalformedURLException {
    this.canonicalUrl = new URL(canonicalUrl);
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {}

  @Override
  public void doFilter(
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
      throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
    HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
    Optional<String> maybeRedirectURL =
        Optional.ofNullable(httpRequest.getHeader(X_FORWARDED_HOST_HTTP_HEADER))
            .map(xfhHeader -> buildRedirectionUrl(xfhHeader));

    if (maybeRedirectURL.isPresent()) {
      logger.info("Redirecting to " + maybeRedirectURL.get());
      httpResponse.sendRedirect(maybeRedirectURL.get());
    } else {
      filterChain.doFilter(servletRequest, servletResponse);
    }
  }

  @Override
  public void destroy() {}

  private String buildRedirectionUrl(String xfhHeader) {
    if (canonicalUrl.getHost().equalsIgnoreCase(xfhHeader)) {
      return "/login";
    } else {
      int canonicalPort = canonicalUrl.getPort();
      String newPort = (canonicalPort == -1) ? "80" : String.valueOf(canonicalPort);
      String finalRedirectUrl = canonicalUrl.getProtocol() + "://" + xfhHeader + ":" + newPort;
      try {
        return canonicalUrl
            + "login?final_redirect="
            + URLEncoder.encode(finalRedirectUrl, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e); // TODO deal with this
      }
    }
  }
}
