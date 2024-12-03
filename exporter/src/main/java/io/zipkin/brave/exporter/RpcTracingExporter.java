/*
 * Copyright The OpenZipkin Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.zipkin.brave.exporter;

import brave.Tracing;
import brave.rpc.RpcTracing;
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
    name = "io.zipkin.brave.rpc"
)
public class RpcTracingExporter {
  @Reference Tracing tracing;

  private RpcTracing rpcTracing;
  private ServiceRegistration<RpcTracing> reg;

  @Activate public void activate(BundleContext context, Map<String, String> properties) {
    rpcTracing = RpcTracing.newBuilder(tracing).build();
    reg = context.registerService(RpcTracing.class, rpcTracing,
        new Hashtable<String, String>(properties));
  }

  @Deactivate public void deactive() {
    reg.unregister();
    if (rpcTracing != null) rpcTracing.close();
  }
}
