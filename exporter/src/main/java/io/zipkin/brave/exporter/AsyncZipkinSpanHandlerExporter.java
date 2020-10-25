/*
 * Copyright 2016-2020 The OpenZipkin Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
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
import zipkin2.reporter.Sender;
import zipkin2.reporter.brave.AsyncZipkinSpanHandler;

@Component(
    immediate = true,
    name = "io.zipkin.brave.asynczipkinspanhandler"
)
public class AsyncZipkinSpanHandlerExporter {
  @Reference Sender sender;

  private AsyncZipkinSpanHandler zipkinSpanHandler;
  @SuppressWarnings("rawtypes")
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
