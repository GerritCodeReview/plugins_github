package com.googlesource.gerrit.plugins.github.notification;

import java.io.IOException;

import org.kohsuke.github.GHEvent;

/**
 * Abstract interface to handler which is responsible for a specific github
 * webhook event type.
 *
 * Implementation classes must be named by the convention which
 * {@link WebhookServlet#getWebhookClassName(GHEvent)} defines.
 *
 * @param <T> Type of payload. Must be consistent to the event type.
 * 
 * @return true if the event has been successfully processed
 */
interface WebhookEventHandler<T> {
  Class<T> getPayloadType();

  boolean doAction(T payload)
      throws IOException;
}