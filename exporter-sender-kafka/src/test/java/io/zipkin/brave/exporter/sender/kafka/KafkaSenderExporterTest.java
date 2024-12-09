/*
 * Copyright The OpenZipkin Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.zipkin.brave.exporter.sender.kafka;

import io.zipkin.brave.exporter.sender.kafka.KafkaSenderExporter.Config;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import zipkin2.reporter.Encoding;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Config(bootstrapServers = "server1", messageMaxBytes = 10, topic = "mytopic")
public class KafkaSenderExporterTest {

  @Test public void testConfig() {
    KafkaSenderExporter exporter = new KafkaSenderExporter();
    Map<String, String> properties = new HashMap<String, String>();
    properties.put("kafka.myprop", "myvalue");
    BundleContext context = mock(BundleContext.class);
    Config config = mock(Config.class);
    when(config.bootstrapServers()).thenReturn("server1");
    when(config.encoding()).thenReturn(Encoding.JSON);
    when(config.topic()).thenReturn("mytopic");
    exporter.activate(config, context, properties);
  }
}
