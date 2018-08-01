/*
 * Copyright 2016-2018 The OpenZipkin Authors
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

import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import zipkin2.Span;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.Sender;

@Component(
    immediate = true,
    name = "io.zipkin.asyncreporter"
)
public class AsyncReporterExporter {
  @Reference
  Sender sender;

  private AsyncReporter<Span> reporter;
  @SuppressWarnings("rawtypes")
  private ServiceRegistration<Reporter> reg;

  @Activate
  public void activate(BundleContext context, Map<String, String> properties) {
    reporter = AsyncReporter.builder(sender)
        .build();
    reg = context.registerService(Reporter.class, reporter,
        new Hashtable<String, String>(properties));
  }

  @Deactivate
  public void deactive() {
    reg.unregister();
    if (reporter != null) reporter.close();
  }
}
