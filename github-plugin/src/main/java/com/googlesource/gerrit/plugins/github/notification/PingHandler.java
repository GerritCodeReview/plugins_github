package com.googlesource.gerrit.plugins.github.notification;

import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.kohsuke.github.GHEventPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.Singleton;

// Handles ping event in github webhook.
// @see <a href="https://developer.github.com/webhooks/#ping-event">Ping
// Event</a>
@Singleton
class PingHandler implements EventHandler<PingHandler.Ping> {
  private static Logger logger = LoggerFactory.getLogger(PingHandler.class);

  @JsonAutoDetect
  static class Ping {
    @JsonProperty("zen")
    String zen;
    @JsonProperty("hook_id")
    int hookId;
  }

  @Override
  public void doAction(Ping payload, HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    logger.info("ping id={} zen={}", payload.zen, payload.hookId);
    resp.setStatus(SC_NO_CONTENT);
  }

  @Override
  public Class<Ping> getPayloadType() {
    return Ping.class;
  }
}
