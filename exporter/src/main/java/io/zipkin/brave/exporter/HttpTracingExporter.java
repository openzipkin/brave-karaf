/*
 * Copyright The OpenZipkin Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.zipkin.brave.exporter;

import brave.Tracing;
import brave.http.HttpTracing;
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
    name = "io.zipkin.brave.http"
)
public class HttpTracingExporter {
  @Reference Tracing tracing;

  private HttpTracing httpTracing;
  private ServiceRegistration<HttpTracing> reg;

  @Activate public void activate(BundleContext context, Map<String, String> properties) {
    httpTracing = HttpTracing.newBuilder(tracing).build();
    reg = context.registerService(HttpTracing.class, httpTracing,
        new Hashtable<String, String>(properties));
  }

  @Deactivate public void deactive() {
    reg.unregister();
    if (httpTracing != null) httpTracing.close();
  }
}
