package com.googlesource.gerrit.plugins.github.oauth;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;
import com.google.common.io.CharStreams;
import com.google.gerrit.extensions.auth.oauth.OAuthToken;
import com.google.gerrit.extensions.auth.oauth.OAuthVerifier;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
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
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class OAuthProtocol {
  /**
   * Supported OAuth Scopes.
   *
   * <p>OAuth authorization scopes as defined in GitHub API at:
   * https://developer.github.com/v3/oauth/#scopes
   */
  public static enum Scope {
    /**
     * Grants read-only access to public information (includes public user profile info, public
     * repository info, and gists)
     */
    DEFAULT("", "Read public information"),

    /**
     * Grants read/write access to profile info only. Note that this scope includes user:email and
     * user:follow.
     */
    USER("user", "Read/write profile info"),

    /** Grants read access to a user’s email addresses. */
    USER_EMAIL("user:email", "Access e-mail addresses"),

    /** Grants access to follow or unfollow other users. */
    USER_FOLLOW("user:follow", "(Un)Follow users"),

    /**
     * Grants read/write access to code, commit statuses, collaborators, and deployment statuses for
     * public repositories. Also required for starring public repositories.
     */
    PUBLIC_REPO("public_repo", "Read and push to public repos"),

    /**
     * Grants read/write access to code, commit statuses, collaborators, and deployment statuses for
     * public and private repositories and organizations.
     */
    REPO("repo", "Read and push to public and private repos"),

    /**
     * Grants access to deployment statuses for public and private repositories. This scope is only
     * necessary to grant other users or services access to deployment statuses, without granting
     * access to the code.
     */
    REPO_DEPLOYMENT("repo_deployment", "Read deployment statuses"),

    /**
     * Grants read/write access to public and private repository commit statuses. This scope is only
     * necessary to grant other users or services access to private repository commit statuses
     * without granting access to the code.
     */
    REPO_STATUS("repo:status", "Read commit statuses"),

    /** Grants access to delete admin-able repositories. */
    DELETE_REPO("delete_repo", "Delete repos"),

    /** Grants read access to a user’s notifications. repo also provides this access. */
    NOTIFICATIONS("notifications", "Read notifications"),

    /** Grants write access to gists. */
    GIST("gist", "Write Gists"),

    /** Grants read and ping access to hooks in public or private repositories. */
    READ_REPO_HOOK("read:repo_hook", "Read/ping public/private repos' hooks"),

    /** Grants read, write, and ping access to hooks in public or private repositories. */
    WRITE_REPO_HOOK("write:repo_hook", "Read/write/ping public/private repos' hooks"),

    /** Grants read, write, ping, and delete access to hooks in public or private repositories. */
    ADMIN_REPO_HOOK(
        "admin:repo_hook", "Read/write/ping/delete access to public/private repos' hooks"),

    /**
     * Grants read, write, ping, and delete access to organization hooks. Note: OAuth tokens will
     * only be able to perform these actions on organization hooks which were created by the OAuth
     * application. Personal access tokens will only be able to perform these actions on
     * organization hooks created by a user.
     */
    ADMIN_ORG_HOOK(
        "admin:org_hook", "Read/write/ping/delete access to public/private organizations' hooks"),

    /** Read-only access to organization, teams, and membership. */
    READ_ORG("read:org", "Read membership to public/private organizations"),

    /** Publicize and un-publicize organization membership. */
    WRITE_ORG("write:org", "(Un)Publicize organizations membership"),

    /** Fully manage organization, teams, and memberships. */
    ADMIN_ORG("admin:org", "Manage organizations, teams and memberships"),

    /** List and view details for public keys. */
    READ_PUBLIC_KEY("read:public_key", "Read owned public keys"),

    /** Create, list, and view details for public keys. */
    WRITE_PUBLIC_KEY("write:public_key", "Read/write/list owned public keys"),

    /** Fully manage public keys. */
    ADMIN_PUBLIC_KEY("admin:public_key", "Fully manage owned public keys"),

    /** Grants the ability to add and update GitHub Actions workflow files. */
    WORKFLOW("workflow", "Manage actions workflow files.");

    public final String value;

    public final String description;

    Scope(final String value, final String description) {
      this.value = value;
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  private static final String ME_SEPARATOR = ",";
  private static final Logger log = LoggerFactory.getLogger(OAuthProtocol.class);
  private static final String FINAL_URL_PARAM = "final";
  private static SecureRandom randomState = newRandomGenerator();

  private final GitHubOAuthConfig config;
  private final CanonicalWebUrls canonicalWebUrls;
  private final Gson gson;
  private final Provider<HttpClient> httpProvider;

  public static class AccessToken {
    public String accessToken;
    public String tokenType;
    public String error;
    public String errorDescription;
    public String errorUri;
    public String raw;

    public AccessToken() {}

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
        return "Error AccessToken [error="
            + error
            + ", error_description="
            + errorDescription
            + ", error_uri="
            + errorUri
            + "]";
      }
      return "AccessToken [access_token=" + accessToken + ", token_type=" + tokenType + "]";
    }

    @Override
    public int hashCode() {
      return Objects.hash(accessToken, tokenType, error, errorDescription, errorUri);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      final AccessToken other = (AccessToken) obj;
      return Objects.equals(this.accessToken, other.accessToken)
          && Objects.equals(this.raw, other.raw)
          && Objects.equals(this.tokenType, other.tokenType);
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
  public OAuthProtocol(
      GitHubOAuthConfig config,
      CanonicalWebUrls canonicalWebUrls,
      PooledHttpClientProvider httpClientProvider,
      /*
       * We need to explicitly tell Guice which Provider<> we need as this class
       * may be instantiated outside the standard Guice Module set-up (e.g.
       * initial Servlet login filter)
       */
      GsonProvider gsonProvider) {
    this.config = config;
    this.canonicalWebUrls = canonicalWebUrls;
    this.httpProvider = httpClientProvider;
    this.gson = gsonProvider.get();
  }

  private static SecureRandom newRandomGenerator() {
    try {
      return SecureRandom.getInstance("SHA1PRNG");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalArgumentException("No SecureRandom available for GitHub authentication", e);
    }
  }

  public String getAuthorizationUrl(String scopesString, String state, HttpServletRequest req) {
    return config.gitHubOAuthUrl
        + "?client_id="
        + config.gitHubClientId
        + getURLEncodedParameter("&scope=", scopesString)
        + getURLEncodedParameter("&redirect_uri=", canonicalWebUrls.getOAuthFinalRedirectUrl())
        + getURLEncodedParameter("&state=", state);
  }

  public String loginPhase1(
      HttpServletRequest request, HttpServletResponse response, Set<Scope> scopes)
      throws IOException {
    String scopesString = getScope(scopes);
    log.debug(
        "Initiating GitHub Login for ClientId="
            + config.gitHubClientId
            + " Scopes="
            + scopesString);
    String state = newRandomState(request.getRequestURI().toString());
    log.debug(
        "Initiating GitHub Login for ClientId="
            + config.gitHubClientId
            + " Scopes="
            + scopesString);
    response.sendRedirect(getAuthorizationUrl(scopesString, state, request));
    return state;
  }

  public String getScope(Set<Scope> scopes) {
    if (scopes.size() <= 0) {
      return "";
    }

    StringBuilder out = new StringBuilder();
    for (Scope scope : scopes) {
      if (out.length() > 0) {
        out.append(",");
      }
      out.append(scope.value);
    }
    return out.toString();
  }

  public static boolean isOAuthFinal(HttpServletRequest request) {
    return Strings.emptyToNull(request.getParameter("code")) != null
        && request.getParameter(FINAL_URL_PARAM) == null;
  }

  public static boolean isOAuthFinalForOthers(HttpServletRequest request) {
    String targetUrl = getTargetUrl(request);
    if (targetUrl.equals(request.getRequestURI())) {
      return false;
    }

    return Strings.emptyToNull(request.getParameter("code")) != null
        && request.getParameter(FINAL_URL_PARAM) == null;
  }

  public String newRandomState(String redirectUrl) {
    byte[] stateBin = new byte[20]; // SHA-1 size
    randomState.nextBytes(stateBin);
    return BaseEncoding.base64Url().encode(stateBin) + ME_SEPARATOR + redirectUrl;
  }

  public static boolean isOAuthLogin(HttpServletRequest request) {
    String requestUri = request.getRequestURI();
    return requestUri.indexOf(GitHubOAuthConfig.GERRIT_LOGIN) >= 0
        && request.getParameter(FINAL_URL_PARAM) == null;
  }

  public static boolean isOAuthLogout(HttpServletRequest request) {
    return request.getRequestURI().indexOf(GitHubOAuthConfig.GERRIT_LOGOUT) >= 0;
  }

  public static boolean isOAuthRequest(HttpServletRequest httpRequest) {
    return OAuthProtocol.isOAuthLogin(httpRequest) || OAuthProtocol.isOAuthFinal(httpRequest);
  }

  public AccessToken loginPhase2(
      HttpServletRequest request, HttpServletResponse response, String state) throws IOException {
    String requestState = request.getParameter("state");
    if (!Objects.equals(state, requestState)) {
      throw new IOException(
          "Invalid authentication state: expected '" + state + "' but was '" + requestState + "'");
    }

    return getAccessToken(new OAuthVerifier(request.getParameter("code")));
  }

  public AccessToken getAccessToken(OAuthVerifier code) throws IOException {
    HttpPost post = new HttpPost(config.gitHubOAuthAccessTokenUrl);
    post.setHeader("Accept", "application/json");
    List<NameValuePair> nvps = new ArrayList<>();
    nvps.add(new BasicNameValuePair("client_id", config.gitHubClientId));
    nvps.add(new BasicNameValuePair("client_secret", config.gitHubClientSecret));
    nvps.add(new BasicNameValuePair("code", code.getValue()));
    post.setEntity(new UrlEncodedFormEntity(nvps, Charsets.UTF_8));

    HttpResponse postResponse = httpProvider.get().execute(post);
    if (postResponse.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
      log.error(
          "POST "
              + config.gitHubOAuthAccessTokenUrl
              + " request for access token failed with status "
              + postResponse.getStatusLine());
      EntityUtils.consume(postResponse.getEntity());
      throw new IOException("GitHub OAuth request failed");
    }

    InputStream content = postResponse.getEntity().getContent();
    String tokenJsonString =
        CharStreams.toString(new InputStreamReader(content, StandardCharsets.UTF_8));
    AccessToken token = gson.fromJson(tokenJsonString, AccessToken.class);
    if (token.isError()) {
      log.error("POST " + config.gitHubOAuthAccessTokenUrl + " returned an error token: " + token);
      throw new IOException("Invalid GitHub OAuth token");
    }

    return token;
  }

  private static String getURLEncodedParameter(String prefix, String url) {
    try {
      return Strings.isNullOrEmpty(url) ? "" : (prefix + URLEncoder.encode(url, "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("Cannot find UTF-8 encoding", e);
    }
  }

  public static String getTargetUrl(ServletRequest request) {
    int meEnd = state(request).indexOf(ME_SEPARATOR);
    String finalUrlSuffix = "?" + FINAL_URL_PARAM + "=true";
    if (meEnd > 0) {
      return state(request).substring(meEnd + 1) + finalUrlSuffix;
    }
    return finalUrlSuffix;
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
