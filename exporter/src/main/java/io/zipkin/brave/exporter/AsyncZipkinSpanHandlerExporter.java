/*
 * Copyright The OpenZipkin Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.zipkin.brave.exporter;

import brave.handler.SpanHandler;
import java.util.Hashtable;
import java.util.Map;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import zipkin2.reporter.BytesMessageSender;
import zipkin2.reporter.brave.AsyncZipkinSpanHandler;

@Component(
    immediate = true,
    name = "io.zipkin.brave.asynczipkinspanhandler"
)
public class AsyncZipkinSpanHandlerExporter {
  @Reference BytesMessageSender sender;

  private AsyncZipkinSpanHandler zipkinSpanHandler;
  private ServiceRegistration<SpanHandler> reg;

  @Activate public void activate(BundleContext context, Map<String, String> properties) {
    zipkinSpanHandler = AsyncZipkinSpanHandler.newBuilder(sender).build();
    reg = context.registerService(SpanHandler.class, zipkinSpanHandler,
        new Hashtable<String, String>(properties));
  }

  @Deactivate public void deactive() {
    reg.unregister();
    if (zipkinSpanHandler != null) zipkinSpanHandler.close();
  }
}
