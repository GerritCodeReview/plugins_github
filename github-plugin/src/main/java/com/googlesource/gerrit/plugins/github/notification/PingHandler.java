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

import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Handles ping event in github webhook.
 *
 * @see <a href="https://developer.github.com/webhooks/#ping-event">Ping
 *      Event</a>
 */
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
  public void doAction(Ping payload, HttpServletRequest req,
      HttpServletResponse resp) throws IOException {
    logger.info("ping id={} zen={}", payload.zen, payload.hookId);
    resp.setStatus(SC_NO_CONTENT);
  }

  @Override
  public Class<Ping> getPayloadType() {
    return Ping.class;
  }
}
