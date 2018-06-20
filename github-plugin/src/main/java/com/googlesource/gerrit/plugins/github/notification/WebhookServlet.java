// Copyright (C) 2015 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.github.notification;

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.httpd.WebSession;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.github.GitHubConfig;
import com.googlesource.gerrit.plugins.github.oauth.GitHubLogin;
import com.googlesource.gerrit.plugins.github.oauth.ScopedProvider;
import com.googlesource.gerrit.plugins.github.oauth.UserScopedProvider;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles webhook callbacks sent from Github. Delegates requests to implementations of {@link
 * WebhookEventHandler}.
 */
@Singleton
public class WebhookServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LoggerFactory.getLogger(WebhookServlet.class);

  private static final String PACKAGE_NAME = WebhookServlet.class.getPackage().getName();
  private static final String SIGNATURE_PREFIX = "sha1=";
  private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

  private final Gson gson;

  private final Map<String, WebhookEventHandler<?>> handlerByName = new ConcurrentHashMap<>();
  private final Injector injector;
  private final GitHubConfig config;

  private final UserScopedProvider<GitHubLogin> loginProvider;
  private final ScopedProvider<GitHubLogin> requestScopedLoginProvider;
  private final DynamicItem<WebSession> session;

  @Inject
  public WebhookServlet(
      UserScopedProvider<GitHubLogin> loginProvider,
      ScopedProvider<GitHubLogin> requestScopedLoginProvider,
      GitHubConfig config,
      Gson gson,
      DynamicItem<WebSession> session,
      Injector injector) {
    this.loginProvider = loginProvider;
    this.requestScopedLoginProvider = requestScopedLoginProvider;
    this.injector = injector;
    this.config = config;
    this.gson = gson;
    this.session = session;
  }

  private WebhookEventHandler<?> getWebhookHandler(String name) {
    if (name == null) {
      logger.error("Null event name: cannot find any handler for it");
      return null;
    }

    WebhookEventHandler<?> handler = handlerByName.get(name);
    if (handler != null) {
      return handler;
    }

    try {
      String className = eventClassName(name);
      Class<?> clazz = Class.forName(className);
      handler = (WebhookEventHandler<?>) injector.getInstance(clazz);
      handlerByName.put(name, handler);
      logger.info("Loaded {}", clazz.getName());
    } catch (ClassNotFoundException e) {
      logger.error("Handler '" + name + "' not found. Skipping", e);
    }

    return handler;
  }

  private String eventClassName(String name) {
    String[] nameParts = name.split("_");
    List<String> classNameParts =
        Lists.transform(
            Arrays.asList(nameParts),
            new Function<String, String>() {
              @Override
              public String apply(String part) {
                return Character.toUpperCase(part.charAt(0)) + part.substring(1);
              }
            });
    return PACKAGE_NAME + "." + Joiner.on("").join(classNameParts) + "Handler";
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    if (Strings.emptyToNull(config.webhookUser) == null) {
      logger.error("No webhookUser defined: cannot process GitHub events");
      resp.sendError(SC_INTERNAL_SERVER_ERROR);
      return;
    }

    WebhookEventHandler<?> handler = getWebhookHandler(req.getHeader("X-Github-Event"));
    if (handler == null) {
      resp.sendError(SC_NOT_FOUND);
      return;
    }

    try (BufferedReader reader = req.getReader()) {
      String body = Joiner.on("\n").join(CharStreams.readLines(reader));
      if (!validateSignature(req.getHeader("X-Hub-Signature"), body, req.getCharacterEncoding())) {
        logger.error("Signature mismatch to the payload");
        resp.sendError(SC_FORBIDDEN);
        return;
      }

      session.get().setUserAccountId(Account.Id.fromRef(config.webhookUser));
      GitHubLogin login = loginProvider.get(config.webhookUser);
      if (login == null || !login.isLoggedIn()) {
        logger.error(
            "Cannot login to github as {}. {}.webhookUser is not correctly configured?",
            config.webhookUser,
            GitHubConfig.CONF_SECTION);
        resp.setStatus(SC_INTERNAL_SERVER_ERROR);
        return;
      }
      requestScopedLoginProvider.get(req).login(login.getToken());

      if (callHander(handler, body)) {
        resp.setStatus(SC_NO_CONTENT);
      } else {
        resp.sendError(SC_INTERNAL_SERVER_ERROR);
      }
    }
  }

  private <T> boolean callHander(WebhookEventHandler<T> handler, String jsonBody)
      throws IOException {
    T payload = gson.fromJson(jsonBody, handler.getPayloadType());
    if (payload != null) {
      return handler.doAction(payload);
    }
    logger.error(
        "Cannot decode JSON payload '" + jsonBody + "' into " + handler.getPayloadType().getName());
    return false;
  }

  /**
   * validates callback signature sent from github
   *
   * @param signatureHeader signature HTTP request header of a github webhook
   * @param payload HTTP request body
   * @return true if webhook secret is not configured or signatureHeader is valid against payload
   *     and the secret, false if otherwise.
   * @throws UnsupportedEncodingException
   */
  private boolean validateSignature(String signatureHeader, String body, String encoding)
      throws UnsupportedEncodingException {
    byte[] payload = body.getBytes(encoding == null ? "UTF-8" : encoding);
    if (config.webhookSecret == null || config.webhookSecret.equals("")) {
      logger.debug(
          "{}.webhookSecret not configured. Skip signature validation", GitHubConfig.CONF_SECTION);
      return true;
    }

    if (!StringUtils.startsWith(signatureHeader, SIGNATURE_PREFIX)) {
      logger.error("Unsupported webhook signature type: {}", signatureHeader);
      return false;
    }
    byte[] signature;
    try {
      signature = Hex.decodeHex(signatureHeader.substring(SIGNATURE_PREFIX.length()).toCharArray());
    } catch (DecoderException e) {
      logger.error("Invalid signature: {}", signatureHeader);
      return false;
    }
    return MessageDigest.isEqual(signature, getExpectedSignature(payload));
  }

  /**
   * Calculates the expected signature of the payload
   *
   * @param payload payload to calculate a signature for
   * @return signature of the payload
   * @see <a href=
   *     "https://developer.github.com/webhooks/securing/#validating-payloads-from-github">
   *     Validating payloads from GitHub</a>
   */
  private byte[] getExpectedSignature(byte[] payload) {
    SecretKeySpec key = new SecretKeySpec(config.webhookSecret.getBytes(), HMAC_SHA1_ALGORITHM);
    Mac hmac;
    try {
      hmac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
      hmac.init(key);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Hmac SHA1 must be supported", e);
    } catch (InvalidKeyException e) {
      throw new IllegalStateException("Hmac SHA1 must be compatible to Hmac SHA1 Secret Key", e);
    }
    return hmac.doFinal(payload);
  }
}
