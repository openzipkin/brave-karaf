/**
 * Copyright 2016 The OpenZipkin Authors
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
package io.zipkin.brave.osgi.exporter.kafka;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.BundleContext;
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
import zipkin.reporter.kafka08.KafkaSender;

@Component //
( //
    immediate = true, //
    name = "io.zipkin.reporter.kafka08", //
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = KafkaReporterExporter.Config.class)
public class KafkaReporterExporter {

    @SuppressWarnings("rawtypes")
    private ServiceRegistration<Reporter> reg;
    private AsyncReporter<Span> reporter;

    @ObjectClassDefinition(name = "Zipkin Reporter Kafka08")
    @interface Config {
        String bootstrapServers() default "localhost:9092";
        Encoding encoding() default Encoding.THRIFT;
        int messageMaxBytes() default 1000000;
        String topic() default "zipkin";
        String overrides();
    }

    @Activate
    public void activate(Config config, BundleContext context, Map<String,String> properties) {
        config.overrides();
        Map<String, String> overrides = new HashMap<String, String>();
        KafkaSender sender = KafkaSender.builder() //
            .bootstrapServers(config.bootstrapServers()) //
            .encoding(config.encoding()) //
            .messageMaxBytes(config.messageMaxBytes()) //
            .overrides(overrides) //
            .topic(config.topic()) //
            .build();
        reporter = AsyncReporter.builder(sender).build();
        reg = context.registerService(Reporter.class, reporter, new Hashtable<String, String>(properties));
    }
    
    @Deactivate
    public void deactive() {
        reg.unregister();
        reporter.close();
    }

}
