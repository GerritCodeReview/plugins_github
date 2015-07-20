package com.googlesource.gerrit.plugins.github.notification;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.github.GHEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker.Std;
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.httpd.WebSession;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.reviewdb.client.Account.Id;
import com.google.gerrit.server.IdentifiedUser;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.github.GitHubConfig;
import com.googlesource.gerrit.plugins.github.git.PullRequestImporter;
import com.googlesource.gerrit.plugins.github.oauth.GitHubLogin;
import com.googlesource.gerrit.plugins.github.oauth.ScopedProvider;
import com.googlesource.gerrit.plugins.github.oauth.OAuthProtocol.AccessToken;

@Singleton
public class WebhookServlet extends HttpServlet {
  private static final long serialVersionUID = 7495146132877215533L;
  private static final Logger logger =
      LoggerFactory.getLogger(WebhookServlet.class);

  private static final String PACKAGE_NAME =
      WebhookServlet.class.getPackage().getName();
  private static final String SIGNATURE_PREFIX = "sha1";
  private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

  private static final ObjectMapper mapper;

  static {
    mapper = new ObjectMapper();
    mapper.setVisibilityChecker(new Std(NONE, NONE, NONE, NONE, ANY));
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  private final Map<String, Class<? extends EventHandler<?>>> handlerByName =
      new HashMap<String, Class<? extends EventHandler<?>>>();
  private final Injector injector;
  private final GitHubConfig config;

  private final ScopedProvider<GitHubLogin> loginProvider;
  private final IdentifiedUser.GenericFactory userFactory;
  private final DynamicItem<WebSession> session;


  @Inject
  public WebhookServlet(final ScopedProvider<GitHubLogin> loginProvider,
      final Injector injector, final GitHubConfig config,
      final IdentifiedUser.GenericFactory userFactory,
      final DynamicItem<WebSession> session
      ) {
    this.loginProvider = loginProvider;
    this.injector = injector;
    this.config = config;
    this.userFactory = userFactory;
    this.session = session;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void init() throws ServletException {
    super.init();
    handlerByName.put("ping", PingHandler.class);
    logger.info("Loaded PingHandler");
    for (GHEvent event : GHEvent.values()) {
      String name = getControllerClassName(event);
      Class<? extends EventHandler<?>> clazz;
      try {
        clazz = (Class<? extends EventHandler<?>>) Class.forName(name);
      } catch (ClassNotFoundException e) {
        logger.debug("Handler \"{}\" not found. Skipping", name);
        continue;
      }
      logger.info("Loaded {}", clazz.getName());
      handlerByName.put(event.name().toLowerCase(), clazz);
    }
  }

  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String name = req.getHeader("X-Github-Event");
    Class<? extends EventHandler<?>> clazz = handlerByName.get(name);
    if (clazz == null) {
      logger.debug("no handler for event {}", name);
      resp.sendError(SC_NOT_FOUND);
      return;
    }
    // HttpServletRequest rc = injector.getInstance(HttpServletRequest.class);
    EventHandler<?> handler = injector.getInstance(clazz);
    byte[] body = IOUtils.toByteArray(req.getInputStream());
    if (!validateSignature(req.getHeader("X-Github-Signature"), body)) {
      logger.error("Signature mismatch to the payload");
      resp.setStatus(SC_BAD_REQUEST);
      return;
    }

    Id id = Account.Id.fromRef("yugui-gerrit-example");
    IdentifiedUser user = userFactory.create(id);
    session.get().setUserAccountId(id);
    GitHubLogin login = loginProvider.get(req);
    if (!login.isLoggedIn()) {
      login.login(new AccessToken("xxxxxxxxxxx"));
    }
    
    callHander(handler, body, req, resp);
  }

  private <T> void callHander(EventHandler<T> handler, byte[] body,
      HttpServletRequest req, HttpServletResponse resp) throws IOException {
    T payload;
    try {
      payload = mapper.readValue(body, handler.getPayloadType());
    } catch (IOException e) {
      logger.error("invalid payload", e);
      resp.sendError(SC_BAD_REQUEST);
      return;
    }

    handler.doAction(payload, req, resp);
  }

  private static String getControllerClassName(GHEvent event) {
    StringBuilder controllerName = new StringBuilder(PACKAGE_NAME);
    controllerName.append(".");
    for (String component : event.name().split("_")) {
      controllerName.append(component.charAt(0));
      controllerName.append(component.substring(1).toLowerCase());
    }
    controllerName.append("Handler");
    return controllerName.toString();
  }

  // validates callback signature sent from github
  // @param signatureHeader signature HTTP request header of a github webhook
  // @param payload HTTP request body
  // @return true if webhook secret is not configured or signatureHeade is valid
  // against payload and the secret, false if otherwise.
  private boolean validateSignature(String signatureHeader, byte[] payload) {
    if (config.webhookSecret == null || config.webhookSecret.equals("")) {
      logger.debug("{}.webhookSecret not configured. Skip signature validation",
          GitHubConfig.CONF_SECTION);
      return true;
    }

    if (!StringUtils.startsWith(signatureHeader, SIGNATURE_PREFIX)) {
      logger.error("Unsupported webhook signature type: {}", signatureHeader);
      return false;
    }
    byte[] signature;
    try {
      signature = Hex.decodeHex(
          signatureHeader.substring(SIGNATURE_PREFIX.length()).toCharArray());
    } catch (DecoderException e) {
      logger.error("Invalid signature: {}", signatureHeader);
      return false;
    }
    return MessageDigest.isEqual(signature, getExpectedSignature(payload));
  }

  private byte[] getExpectedSignature(byte[] payload) {
    SecretKeySpec key =
        new SecretKeySpec(config.webhookSecret.getBytes(), HMAC_SHA1_ALGORITHM);
    Mac hmac;
    try {
      hmac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
      hmac.init(key);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Hmac SHA1 must be supported", e);
    } catch (InvalidKeyException e) {
      throw new IllegalStateException(
          "Hmac SHA1 must be compatible to Hmac SHA1 Secret Key", e);
    }
    return hmac.doFinal(payload);
  }
}


interface EventHandler<T> {
  Class<T> getPayloadType();

  void doAction(T payload, HttpServletRequest req, HttpServletResponse resp) throws IOException;
}
