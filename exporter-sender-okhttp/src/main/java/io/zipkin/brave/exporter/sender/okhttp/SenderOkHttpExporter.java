/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import zipkin2.reporter.Sender;
import zipkin2.reporter.okhttp3.OkHttpSender;

@Component(
    immediate = true,
    name = "io.zipkin.sender.okhttp",
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = SenderOkHttpExporter.Config.class)
public class SenderOkHttpExporter {

  private ServiceRegistration<Sender> reg;
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
    reg = context.registerService(Sender.class, sender, new Hashtable<>(properties));
  }

  @Deactivate
  public void deactive() {
    reg.unregister();
    if (sender != null) sender.close();
  }
}