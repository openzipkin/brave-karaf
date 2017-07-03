/**
 * Copyright 2016-2017 The OpenZipkin Authors
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
package io.zipkin.brave.osgi.exporter.okhttp3.internal;

import brave.Tracing;
import brave.context.slf4j.MDCCurrentTraceContext;
import brave.sampler.Sampler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import zipkin.Span;
import zipkin.reporter.AsyncReporter;
import zipkin.reporter.Encoding;
import zipkin.reporter.Reporter;
import zipkin.reporter.okhttp3.OkHttpSender;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

@Component(
        immediate = true,
        name = "io.zipkin.reporter.okhttp3",
        property = {"sender=okhttp3"},
        configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = OkHttp3ReporterExporter.Config.class)
public class OkHttp3ReporterExporter {

    private static final String OVERRIDE_PREFIX = "okhttp3.";

    @SuppressWarnings("rawtypes")
    private ServiceRegistration<Reporter> reporterServiceRegistration;
    @SuppressWarnings("rawtypes")
    private ServiceRegistration<Tracing> tracingServiceRegistration;

    private AsyncReporter<Span> reporter;
    private Tracing tracing;

    private BundleContext context;

    @ObjectClassDefinition(name = "Zipkin Reporter OkHttp3")
    @interface Config {
        String zipkinUrl() default "localhost:9411";

        boolean compressionEnabled() default true;

        Encoding encoding() default Encoding.THRIFT;

        int messageMaxBytes() default 1000000;

        String localServiceName() default "zipkin-okhttp3";

        String samplerFilter() default "(type=always)";
    }

    @Activate
    public void activate(Config config, BundleContext context, Map<String, String> properties) {
        this.context = context;
        OkHttpSender sender = createSender(config, properties);
        reporter = AsyncReporter.create(sender);

        reporterServiceRegistration = context.registerService(Reporter.class, reporter,
                new Hashtable<String, String>(properties));

        tracing = httpTracer(config, properties, reporter);
        tracingServiceRegistration = context.registerService(Tracing.class, tracing,
                new Hashtable<String, String>(properties));
    }

    @SuppressWarnings("unchecked")
    public Tracing httpTracer(Config config, Map<String, String> properties, Reporter reporter) {

        Sampler sampler = Sampler.ALWAYS_SAMPLE;

        try {
            ServiceReference<Sampler>[] samplers =
                    (ServiceReference<Sampler>[]) context.getAllServiceReferences(
                            Sampler.class.getName(), config.samplerFilter());
            if (samplers != null && samplers.length > 0) {
                sampler = context.getService(samplers[0]);
            }

        } catch (InvalidSyntaxException e) {

        }

        Tracing.Builder tracingBuilder = Tracing.newBuilder();
        tracingBuilder.localServiceName(config.localServiceName());
        tracingBuilder.sampler(sampler);
        tracingBuilder.reporter(reporter);
        tracingBuilder.currentTraceContext(MDCCurrentTraceContext.create());

        return tracingBuilder.build();
    }


    OkHttpSender createSender(Config config, Map<String, String> properties) {
        return OkHttpSender.builder()
                .endpoint(config.zipkinUrl())
                .compressionEnabled(config.compressionEnabled())
                .messageMaxBytes(config.messageMaxBytes())
                .build();
    }

    private HashMap<String, String> getOverrides(Map<String, String> properties) {
        HashMap<String, String> overrides = new HashMap<String, String>();
        for (String key : properties.keySet()) {
            if (key.startsWith(OVERRIDE_PREFIX)) {
                overrides.put(key.substring(OVERRIDE_PREFIX.length() - 1), properties.get(key));
            }
        }
        return overrides;
    }

    @Deactivate
    public void deactive() {
        tracingServiceRegistration.unregister();
        reporterServiceRegistration.unregister();
        reporter.close();
    }
}
