package com.googlesource.gerrit.plugins.github.oauth;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class OAuthProtocol {
  private static final Logger log = LoggerFactory.getLogger(OAuthProtocol.class);
  
  private final OAuthConfig config;
  private final HttpClient http;
  private final Gson gson;
  
  public static class AccessToken {
    public String access_token;
    public String token_type;
  }
  
  @Inject
  public OAuthProtocol(OAuthConfig config, HttpClient httpClient, Gson gson) {
    this.config = config;
    this.http = httpClient;
    this.gson = gson;
  }

  public void loginPhase1(HttpServletRequest request,
      HttpServletResponse response) throws IOException {
    response.sendRedirect(String.format(
        "%s?client_id=%s&redirect_uri=%s&state=%s", config.gitHubOAuthUrl,
        config.gitHubClientId, getURLEncoded(config.oAuthFinalRedirectUrl),
        getURLEncoded(request.getRequestURI().toString())));
  }
  
  public boolean isOAuthFinal(HttpServletRequest request) {
    return request.getRequestURI().endsWith(OAuthConfig.OAUTH_FINAL);
  }
  
  public boolean isOAuthLogin(HttpServletRequest request) {
    return request.getRequestURI().endsWith(OAuthConfig.OAUTH_LOGIN);
  }
  
  public GitHubLogin loginPhase2(HttpServletRequest request,
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
      GitHub github = GitHub.connectUsingOAuth(token.access_token);
      return new GitHubLogin(github, token);
    } catch (IOException e) {
      log.error("POST " + config.gitHubOAuthAccessTokenUrl
          + " request for access token failed", e);
      response.sendError(HttpURLConnection.HTTP_UNAUTHORIZED,
          "Request for access token not authorised");
      return null;
    }
  }
  
  private String getURLEncoded(String url) {
    try {
      return URLEncoder.encode(url, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      // UTF-8 is hardcoded, cannot fail
      return null;
    }
  }
}
