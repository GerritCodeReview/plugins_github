package com.googlesource.gerrit.plugins.github.oauth;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class OAuthProtocol {
  
  public static enum Scope {
    DEFAULT(""),
    USER("user"),
    USER_EMAIL("user:email"),
    USER_FOLLOW("user:follow"),
    PUBLIC_REPO("public_repo"),
    REPO("repo"),
    REPO_STATUS("repo_status"),
    DELETE_REPO("delete_repo"),
    NOTIFICATIONS("notifications"),
    GIST("gist");

    private final String value;

    public String getValue() {
      return value;
    }

    private Scope(final String value) {
      this.value = value;
    }
  }
  private static final String ME_SEPARATOR = ",";

  private static final Logger log = LoggerFactory
      .getLogger(OAuthProtocol.class);

  private final GitHubOAuthConfig config;
  private final HttpClient http;
  private final Gson gson;

  public static class AccessToken {
    public String access_token;
    public String token_type;

    public AccessToken() {
    }

    public AccessToken(String token, String type, Scope... scopes) {
      this.access_token = token;
      this.token_type = type;
    }

    @Override
    public String toString() {
      return "AccessToken [access_token=" + access_token + ", token_type="
          + token_type + "]";
    }
  }

  @Inject
  public OAuthProtocol(GitHubOAuthConfig config, GitHubHttpProvider httpClientProvider, Gson gson) {
    this.config = config;
    this.http = httpClientProvider.get();
    this.gson = gson;
  }

  public void loginPhase1(HttpServletRequest request,
      HttpServletResponse response, Scope... scopes) throws IOException {
    String scopeRequested = request.getParameter("scope");
    String baseScopeKey = Objects.firstNonNull(scopeRequested, "scopes");
    response.sendRedirect(String.format(
        "%s?client_id=%s%s&redirect_uri=%s&state=%s%s", config.gitHubOAuthUrl,
        config.gitHubClientId, getScope(baseScopeKey, scopes),
        getURLEncoded(config.oAuthFinalRedirectUrl),
        me(), getURLEncoded(request.getRequestURI().toString())));
  }

  private String getScope(String baseScopeKey, Scope[] scopes) {
    List<Scope> baseScopes = config.scopes.get(baseScopeKey);
    if(baseScopes == null) {
      throw new IllegalArgumentException("Requested OAuth base scope id " + baseScopeKey + " is not configured in gerrit.config");
    }

    HashSet<Scope> fullScopes = new HashSet<OAuthProtocol.Scope>(baseScopes);
    fullScopes.addAll(Arrays.asList(scopes));
    
    if(fullScopes.size() <= 0) {
      return "";
    }
    
    StringBuilder out = new StringBuilder();
    for (Scope scope : fullScopes) {
      if(out.length() > 0) {
        out.append(",");
      }
      out.append(scope.getValue());
    }
    return "&" + "scope=" + out.toString();
  }

  public static boolean isOAuthFinal(HttpServletRequest request) {
    return Strings.emptyToNull(request.getParameter("code")) != null;
  }
  
  public static boolean isOAuthFinalForOthers(HttpServletRequest request) {
    String targetUrl = getTargetUrl(request);
    if(targetUrl.equals(request.getRequestURI())) {
      return false;
    }
    
    return Strings.emptyToNull(request.getParameter("code")) != null;
  }

  public String me() {
    return "" + hashCode() + ME_SEPARATOR;
  }

  public static boolean isOAuthLogin(HttpServletRequest request) {
    return request.getRequestURI().indexOf(GitHubOAuthConfig.OAUTH_LOGIN) >= 0;
  }

  public static boolean isOAuthLogout(HttpServletRequest request) {
    return request.getRequestURI().indexOf(GitHubOAuthConfig.OAUTH_LOGOUT) >= 0;
  }

  public AccessToken loginPhase2(HttpServletRequest request,
      HttpServletResponse response) throws IOException {

    HttpPost post = null;

    post = new HttpPost(config.gitHubOAuthAccessTokenUrl);
    post.setHeader("Accept", "application/json");
    List<NameValuePair> nvps = new ArrayList<NameValuePair>();
    nvps.add(new BasicNameValuePair("client_id", config.gitHubClientId));
    nvps.add(new BasicNameValuePair("client_secret", config.gitHubClientSecret));
    nvps.add(new BasicNameValuePair("code", request.getParameter("code")));
    try {
      post.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
    } catch (UnsupportedEncodingException e) {
      // Will never happen
    }

    try {
      HttpResponse postResponse = http.execute(post);
      if (postResponse.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
        log.error("POST " + config.gitHubOAuthAccessTokenUrl
            + " request for access token failed with status "
            + postResponse.getStatusLine());
        response.sendError(HttpURLConnection.HTTP_UNAUTHORIZED,
            "Request for access token not authorised");
        postResponse.getEntity().consumeContent();
        return null;
      }

      AccessToken token =
          gson.fromJson(new InputStreamReader(postResponse.getEntity()
              .getContent(), "UTF-8"), AccessToken.class);
      return token;
    } catch (IOException e) {
      log.error("POST " + config.gitHubOAuthAccessTokenUrl
          + " request for access token failed", e);
      response.sendError(HttpURLConnection.HTTP_UNAUTHORIZED,
          "Request for access token not authorised");
      return null;
    }
  }

  private static String getURLEncoded(String url) {
    try {
      return URLEncoder.encode(url, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      // UTF-8 is hardcoded, cannot fail
      return null;
    }
  }

  public static String getTargetUrl(ServletRequest request) {
    int meEnd = state(request).indexOf(ME_SEPARATOR);
    if (meEnd > 0) {
      return state(request).substring(meEnd+1);
    } else {
      return "";
    }
  }

  private static String state(ServletRequest request) {
    return Strings.nullToEmpty(request.getParameter("state"));
  }

  public static String getTargetOAuthFinal(HttpServletRequest httpRequest) {
    String targetUrl = getTargetUrl(httpRequest);
    String code = getURLEncoded(httpRequest.getParameter("code"));
    String state = getURLEncoded(httpRequest.getParameter("state"));
    return targetUrl + (targetUrl.indexOf('?') < 0 ? '?' : '&') + "code="
        + code + "&state=" + state;
  }

  @Override
  public String toString() {
    return "OAuthProtocol";
  }

}
