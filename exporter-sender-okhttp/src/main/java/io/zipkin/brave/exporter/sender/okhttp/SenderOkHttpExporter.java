/*
 * Copyright The OpenZipkin Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.zipkin.brave.exporter.sender.okhttp;

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
import zipkin2.reporter.okhttp3.OkHttpSender;

@Component(
    immediate = true,
    name = "io.zipkin.sender.okhttp",
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = SenderOkHttpExporter.Config.class)
public class SenderOkHttpExporter {

  private ServiceRegistration<BytesMessageSender> reg;
  private OkHttpSender sender;

  @ObjectClassDefinition(name = "Zipkin Sender OkHttp") @interface Config {
    String endpoint() default "http://localhost:9411/api/v2/spans";

    boolean compressionEnabled() default true;

    int messageMaxBytes() default 5 * 1024 * 1024;
  }

  @Activate
  public void activate(Config config, BundleContext context, Map<String, String> properties) {
    sender = OkHttpSender.newBuilder()
        .endpoint(config.endpoint())
        .compressionEnabled(config.compressionEnabled())
        .messageMaxBytes(config.messageMaxBytes())
        .build();
    reg = context.registerService(BytesMessageSender.class, sender, new Hashtable<>(properties));
  }

  @Deactivate
  public void deactive() {
    reg.unregister();
    if (sender != null) sender.close();
  }
}