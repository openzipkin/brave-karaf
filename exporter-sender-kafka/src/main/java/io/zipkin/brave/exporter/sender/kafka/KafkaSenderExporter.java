/*
 * Copyright The OpenZipkin Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.zipkin.brave.exporter.sender.kafka;

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
import zipkin2.reporter.Encoding;
import zipkin2.reporter.BytesMessageSender;
import zipkin2.reporter.kafka.KafkaSender;

@Component(
    immediate = true,
    name = "io.zipkin.sender.kafka",
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = KafkaSenderExporter.Config.class)
public class KafkaSenderExporter {

  private static final String OVERRIDE_PREFIX = "kafka.";
  private ServiceRegistration<BytesMessageSender> reg;
  private KafkaSender sender;

  @Activate
  public void activate(Config config, BundleContext context, Map<String, String> properties) {
    sender = KafkaSender.newBuilder()
        .bootstrapServers(config.bootstrapServers())
        .encoding(config.encoding())
        .messageMaxBytes(config.messageMaxBytes())
        .overrides(getOverrides(properties))
        .topic(config.topic())
        .build();
    reg = context.registerService(BytesMessageSender.class, sender, new Hashtable<String, String>(properties));
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

  @Deactivate public void deactive() {
    reg.unregister();
    sender.close();
  }

  @ObjectClassDefinition(name = "Zipkin Sender Kafka") @interface Config {
    String bootstrapServers() default "localhost:9092";

    Encoding encoding() default Encoding.JSON;

    int messageMaxBytes() default 1000000;

    String topic() default "zipkin";
  }
}
