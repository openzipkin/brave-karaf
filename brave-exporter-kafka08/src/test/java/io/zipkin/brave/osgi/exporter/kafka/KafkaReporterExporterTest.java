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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import io.zipkin.brave.osgi.exporter.kafka.KafkaReporterExporter.Config;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import zipkin.reporter.Encoding;


@Config(bootstrapServers = "server1", messageMaxBytes = 10, topic="mytopic")
public class KafkaReporterExporterTest {

    @Test
    public void testConfig() {
        KafkaReporterExporter exporter = new KafkaReporterExporter();
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("kafka.myprop", "myvalue");
        BundleContext context = mock(BundleContext.class);
        Config config = mock(Config.class);
        when(config.bootstrapServers()).thenReturn("server1");
        when(config.encoding()).thenReturn(Encoding.THRIFT);
        when(config.topic()).thenReturn("mytopic");
        exporter.activate(config, context, properties);
    }

}
