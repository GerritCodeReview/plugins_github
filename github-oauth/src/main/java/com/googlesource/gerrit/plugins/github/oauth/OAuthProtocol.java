package com.googlesource.gerrit.plugins.github.oauth;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;
import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gerrit.extensions.auth.oauth.OAuthToken;
import com.google.gerrit.extensions.auth.oauth.OAuthVerifier;

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
    GIST("gist"),
    READ_ORG("read:org");

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
  private static SecureRandom randomState = newRandomGenerator();

  private final GitHubOAuthConfig config;
  private final Gson gson;
  private final Provider<HttpClient> httpProvider;

  public static class AccessToken {
    public String accessToken;
    public String tokenType;
    public String error;
    public String errorDescription;
    public String errorUri;
    public String raw;

    public AccessToken() {
    }

    public AccessToken(String token) {
      this(token, "");
    }

    public AccessToken(String token, String type) {
      this();
      this.accessToken = token;
      this.tokenType = type;
    }

    @Override
    public String toString() {
      if (isError()) {
        return "Error AccessToken [error=" + error + ", error_description="
            + errorDescription + ", error_uri=" + errorUri + "]";
      } else {
        return "AccessToken [access_token=" + accessToken + ", token_type="
            + tokenType + "]";
      }
    }

    @Override
    public int hashCode() {
      return Objects.hash(accessToken, tokenType, error, errorDescription,
          errorUri);
    }

    @Override
    public boolean equals(Object obj) {
      return Objects.deepEquals(this, obj);
    }

    public boolean isError() {
      return !Strings.isNullOrEmpty(error);
    }

    public String getRaw() {
      return raw;
    }

    public void setRaw(String raw) {
      this.raw = raw;
    }

    public OAuthToken toOAuthToken() {
      return new OAuthToken(accessToken, null, getRaw());
    }
  }

  @Inject
  public OAuthProtocol(GitHubOAuthConfig config,
      PooledHttpClientProvider httpClientProvider,
      /*
       * We need to explicitly tell Guice which Provider<> we need as this class
       * may be instantiated outside the standard Guice Module set-up (e.g.
       * initial Servlet login filter)
       */
      GsonProvider gsonProvider) {
    this.config = config;
    this.httpProvider = httpClientProvider;
    this.gson = gsonProvider.get();
  }

  private static SecureRandom newRandomGenerator() {
    try {
      return SecureRandom.getInstance("SHA1PRNG");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalArgumentException(
          "No SecureRandom available for GitHub authentication", e);
    }
  }

  public String getAuthorizationUrl(String scopesString, String state) {
    return config.gitHubOAuthUrl + "?client_id=" + config.gitHubClientId
        + getURLEncodedParameter("&scope=", scopesString)
        + getURLEncodedParameter("&redirect_uri=", config.oAuthFinalRedirectUrl)
        + getURLEncodedParameter("&state=", state);
  }

  public String loginPhase1(HttpServletRequest request,
      HttpServletResponse response, Set<Scope> scopes) throws IOException {
    String scopesString = getScope(scopes);
    log.debug("Initiating GitHub Login for ClientId=" + config.gitHubClientId
        + " Scopes=" + scopesString);
    String state = newRandomState(request.getRequestURI().toString());
    log.debug("Initiating GitHub Login for ClientId=" + config.gitHubClientId + " Scopes=" + scopesString);
    response.sendRedirect(getAuthorizationUrl(scopesString, state));
    return state;
  }

  public String getScope(Set<Scope> scopes) {
    if(scopes.size() <= 0) {
      return "";
    }

    StringBuilder out = new StringBuilder();
    for (Scope scope : scopes) {
      if (out.length() > 0) {
        out.append(",");
      }
      out.append(scope.getValue());
    }
    return out.toString();
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

  public String newRandomState(String redirectUrl) {
    byte[] stateBin = new byte[20]; // SHA-1 size
    randomState.nextBytes(stateBin);
    return BaseEncoding.base64Url().encode(stateBin) + ME_SEPARATOR + redirectUrl;
  }

  public static boolean isOAuthLogin(HttpServletRequest request) {
    return request.getRequestURI().indexOf(GitHubOAuthConfig.GERRIT_LOGIN) >= 0;
  }

  public static boolean isOAuthLogout(HttpServletRequest request) {
    return request.getRequestURI().indexOf(GitHubOAuthConfig.GERRIT_LOGOUT) >= 0;
  }

  public static boolean isOAuthRequest(HttpServletRequest httpRequest) {
    return OAuthProtocol.isOAuthLogin(httpRequest) || OAuthProtocol.isOAuthFinal(httpRequest);
  }

  public AccessToken loginPhase2(HttpServletRequest request,
      HttpServletResponse response, String state) throws IOException {
    String requestState = request.getParameter("state");
    if (!Objects.equals(state, requestState)) {
      throw new IOException("Invalid authentication state");
    }

    return getAccessToken(new OAuthVerifier(request.getParameter("code")));
  }

  public AccessToken getAccessToken(OAuthVerifier code) throws IOException {
    HttpPost post = new HttpPost(config.gitHubOAuthAccessTokenUrl);
    post.setHeader("Accept", "application/json");
    List<NameValuePair> nvps = new ArrayList<NameValuePair>();
    nvps.add(new BasicNameValuePair("client_id", config.gitHubClientId));
    nvps.add(new BasicNameValuePair("client_secret", config.gitHubClientSecret));
    nvps.add(new BasicNameValuePair("code", code.getValue()));
    post.setEntity(new UrlEncodedFormEntity(nvps, Charsets.UTF_8));

    HttpResponse postResponse = httpProvider.get().execute(post);
    if (postResponse.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
      log.error("POST " + config.gitHubOAuthAccessTokenUrl
          + " request for access token failed with status "
          + postResponse.getStatusLine());
      EntityUtils.consume(postResponse.getEntity());
      throw new IOException("GitHub OAuth request failed");
    }

    InputStream content = postResponse.getEntity().getContent();
    String tokenJsonString =
        CharStreams.toString(new InputStreamReader(content,
            StandardCharsets.UTF_8));
    AccessToken token = gson.fromJson(tokenJsonString, AccessToken.class);
    if (token.isError()) {
      log.error("POST " + config.gitHubOAuthAccessTokenUrl
          + " returned an error token: " + token);
      throw new IOException("Invalid GitHub OAuth token");
    }

    return token;
  }

  private static String getURLEncodedParameter(String prefix, String url) {
    try {
      return Strings.isNullOrEmpty(url) ? 
          "" : (prefix + URLEncoder.encode(url,"UTF-8"));
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("Cannot find UTF-8 encoding", e);
    }
  }

  public static String getTargetUrl(ServletRequest request) {
    int meEnd = state(request).indexOf(ME_SEPARATOR);
    if (meEnd > 0) {
      return state(request).substring(meEnd + 1);
    } else {
      return "";
    }
  }

  private static String state(ServletRequest request) {
    return Strings.nullToEmpty(request.getParameter("state"));
  }

  public static String getTargetOAuthFinal(HttpServletRequest httpRequest) {
    String targetUrl = getTargetUrl(httpRequest);
    String code = getURLEncodedParameter("code=", httpRequest.getParameter("code"));
    String state = getURLEncodedParameter("&state=", httpRequest.getParameter("state"));
    return targetUrl + (targetUrl.indexOf('?') < 0 ? '?' : '&') + code + state;
  }

  @Override
  public String toString() {
    return "OAuthProtocol";
  }

}
