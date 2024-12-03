/*
 * Copyright The OpenZipkin Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.zipkin.brave.exporter;

import brave.Tracing;
import brave.messaging.MessagingTracing;
import java.util.Hashtable;
import java.util.Map;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Component(
    immediate = true,
    name = "io.zipkin.brave.messaging"
)
public class MessagingTracingExporter {
  @Reference Tracing tracing;

  private MessagingTracing messagingTracing;
  private ServiceRegistration<MessagingTracing> reg;

  @Activate public void activate(BundleContext context, Map<String, String> properties) {
    messagingTracing = MessagingTracing.newBuilder(tracing).build();
    reg = context.registerService(MessagingTracing.class, messagingTracing,
        new Hashtable<String, String>(properties));
  }

  @Deactivate public void deactive() {
    reg.unregister();
    if (messagingTracing != null) messagingTracing.close();
  }
}
