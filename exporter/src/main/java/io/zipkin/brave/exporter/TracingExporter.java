/*
 * Copyright The OpenZipkin Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.zipkin.brave.exporter;

import brave.Tracing;
import brave.handler.SpanHandler;
import brave.sampler.RateLimitingSampler;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(
    immediate = true,
    name = "io.zipkin.brave"
)
@Designate(ocd = TracingExporter.Config.class)
public class TracingExporter {
  @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE) List<SpanHandler> spanHandlers;

  private Tracing tracing;
  private ServiceRegistration<Tracing> reg;

  @Activate
  public void activate(Config config, BundleContext context, Map<String, String> properties) {
    Tracing.Builder builder = Tracing.newBuilder()
        .localServiceName(config.localServiceName())
        .supportsJoin(config.supportsJoin())
        .traceId128Bit(config.traceId128Bit())
        .sampler(RateLimitingSampler.create(config.tracesPerSecond()));
    for (SpanHandler spanHandler : spanHandlers) {
      builder.addSpanHandler(spanHandler);
    }
    tracing = builder.build();
    reg =
        context.registerService(Tracing.class, tracing, new Hashtable<String, String>(properties));
  }

  @Deactivate public void deactive() {
    reg.unregister();
    if (tracing != null) tracing.close();
  }

  public static @ObjectClassDefinition(name = "Tracing") @interface Config {
    String localServiceName() default "unknown";

    boolean supportsJoin() default true;

    boolean traceId128Bit() default false;

    int tracesPerSecond() default 10;
  }
}
