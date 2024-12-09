/*
 * Copyright The OpenZipkin Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.zipkin.brave.exporter.sender.urlconnection;

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

import zipkin2.reporter.BytesMessageSender;
import zipkin2.reporter.urlconnection.URLConnectionSender;

@Component(
    immediate = true,
    name = "io.zipkin.sender.urlconnection",
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = URLConnectionSenderExporter.Config.class)
public class URLConnectionSenderExporter {

  private ServiceRegistration<BytesMessageSender> reg;
  private URLConnectionSender sender;

  @Activate
  public void activate(Config config, BundleContext context, Map<String, String> properties) {
    sender = URLConnectionSender.newBuilder()
        .endpoint(config.endpoint())
        .compressionEnabled(config.compressionEnabled())
        .connectTimeout(config.connectTimeout())
        .messageMaxBytes(config.messageMaxBytes())
        .build();
    reg = context.registerService(BytesMessageSender.class, sender, new Hashtable<String, String>(properties));
  }

  @Deactivate public void deactive() {
    reg.unregister();
    if (sender != null) sender.close();
  }

  public @ObjectClassDefinition(name = "Zipkin Sender URLConnection") @interface Config {
    String endpoint() default "http://localhost:9411/api/v2/spans";

    boolean compressionEnabled() default true;

    int connectTimeout() default 10 * 1000;

    int messageMaxBytes() default 5 * 1024 * 1024;
  }
}