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

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

/**
 * Handles ping event in github webhook.
 *
 * @see <a href="https://developer.github.com/webhooks/#ping-event">Ping
 *      Event</a>
 */
@Singleton
class PingHandler implements WebhookEventHandler<PingHandler.Ping> {
  private static final Logger logger =
      LoggerFactory.getLogger(PingHandler.class);

  static class Ping {
    String zen;
    int hookId;
    
    @Override
    public String toString() {
      return "Ping [zen=" + zen + ", hookId=" + hookId + "]";
    }
  }

  @Override
  public boolean doAction(Ping payload) throws IOException {
    logger.info(payload.toString());
    return true;
  }

  @Override
  public Class<Ping> getPayloadType() {
    return Ping.class;
  }
}
