/*
 * Copyright The OpenZipkin Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.zipkin.brave.itests;

import brave.Tracing;
import brave.handler.SpanHandler;
import brave.http.HttpTracing;
import brave.messaging.MessagingTracing;
import brave.rpc.RpcTracing;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import javax.inject.Inject;
import org.junit.ComparisonFailure;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.options.extra.VMOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import zipkin2.reporter.BytesMessageSender;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.cm.ConfigurationAdminOptions.newConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ITBrave {
  private static final long TIMEOUT = 10000;

  @Inject BundleContext context;

  @Configuration public static Option[] configuration() throws Exception {
    final String karafVersion = getVersionFromMaven("org.apache.karaf.features/org.apache.karaf.features.core");
    MavenArtifactUrlReference karaf = maven().groupId("org.apache.karaf").artifactId("apache-karaf")
        .type("zip")
        .version(karafVersion);
    MavenUrlReference brave =
        maven().groupId("io.zipkin.brave.karaf").artifactId("brave-features").type("xml")
            .classifier("features").version(getBraveKarafVersion());
    return new Option[] {
        karafDistributionConfiguration().frameworkUrl(karaf).useDeployFolder(false),
        configureConsole().ignoreLocalConsole().ignoreRemoteShell(),
        logLevel(LogLevel.INFO),
        keepRuntimeFolder(),
        features(brave, "brave", "brave-sender-kafka", "brave-sender-okhttp"),
        // Create an empty config to trigger creation of component
        newConfiguration("io.zipkin.sender.urlconnection").asOption(),
        newConfiguration("io.zipkin.sender.okhttp").asOption(),
        newConfiguration("io.zipkin.sender.kafka").asOption(),
        newConfiguration("io.zipkin.brave").asOption(),
        newConfiguration("io.zipkin.brave.asynczipkinspanhandler").asOption(),
        newConfiguration("io.zipkin.brave.http").asOption(),
        newConfiguration("io.zipkin.brave.messaging").asOption(),
        newConfiguration("io.zipkin.brave.rpc").asOption(),
        new VMOption("--add-exports=java.base/org.apache.karaf.specs.locator=java.xml,ALL-UNNAMED"),
        new VMOption("--patch-module"),
        new VMOption("java.base=lib/endorsed/org.apache.karaf.specs.locator-" + karafVersion + ".jar"),
        new VMOption("--patch-module"),
        new VMOption("java.xml=lib/endorsed/org.apache.karaf.specs.java.xml-" + karafVersion + ".jar")
    };
  }

  @Test public void checkSenderUrlConnection() {
    ServiceReference<BytesMessageSender> ref =
        getSingle(BytesMessageSender.class, "(component.name=io.zipkin.sender.urlconnection)");

    assertPropertyEquals(ref, "endpoint", "http://localhost:9411/api/v2/spans");
    assertPropertyEquals(ref, "connectTimeout", 10000);
  }

  @Test public void checkSenderKafka() {
    ServiceReference<BytesMessageSender> ref =
        getSingle(BytesMessageSender.class, "(component.name=io.zipkin.sender.kafka)");

    assertPropertyEquals(ref, "topic", "zipkin");
  }

  @Test public void checkSenderOkHttp() {
    ServiceReference<BytesMessageSender> ref =
        getSingle(BytesMessageSender.class, "(component.name=io.zipkin.sender.okhttp)");

    assertPropertyEquals(ref, "endpoint", "http://localhost:9411/api/v2/spans");
  }

  @Test public void checkZipkinSpanHandler() {
    getSingle(SpanHandler.class, "(component.name=io.zipkin.brave.asynczipkinspanhandler)");
  }

  @Test public void checkTracing() {
    ServiceReference<Tracing> ref = getSingle(Tracing.class, "(component.name=io.zipkin.brave)");

    assertPropertyEquals(ref, "tracesPerSecond", 10);

    // It is hard to tell things are configured correctly, except to instantiate the tracer.
    try (Tracing tracing = context.getService(ref)) {
      // Check the span handler actually plumbed. We can't check the type specifically as the test
      // creates many senders, and we don't prioritize which is chosen on conflict.
      //
      // NOTE: This will drift when AsyncZipkinSpanHandler fixes its toString!
      assertTrue(tracing.toString().startsWith("Tracer{spanHandler=AsyncReporter"));
    }
  }

  @Test public void checkHttpTracing() {
    getSingle(HttpTracing.class, "(component.name=io.zipkin.brave.http)");
  }

  @Test public void checkMessagingTracing() {
    getSingle(MessagingTracing.class, "(component.name=io.zipkin.brave.messaging)");
  }

  @Test public void checkRpcTracing() {
    getSingle(RpcTracing.class, "(component.name=io.zipkin.brave.rpc)");
  }

  <T> ServiceReference<T> getSingle(Class<T> type, String filter) {
    long startTime = System.currentTimeMillis();
    while (System.currentTimeMillis() - startTime < TIMEOUT) {
      try {
        Collection<ServiceReference<T>> refs = context.getServiceReferences(type, filter);
        if (refs.size() >= 1) {
          assertEquals(1, refs.size());
          return refs.iterator().next();
        }
        Collection<ServiceReference<T>> allRefs = context.getServiceReferences(type, null);
        if (!allRefs.isEmpty()) {
          System.out.println("filter: " + filter + " didn't match " + allRefs);
        }
        Thread.sleep(100);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    throw new RuntimeException("Timeout finding service: " + filter);
  }

  static String getVersionFromMaven(String path) throws Exception {
    Properties p = new Properties();
    try (InputStream is = ITBrave.class.getResourceAsStream(
        "/META-INF/maven/" + path + "/pom.properties")) {
      p.load(is);
    }
    return p.getProperty("version");
  }

  static String getBraveKarafVersion() throws Exception {
    Properties p = new Properties();
    try (InputStream is = ITBrave.class.getResourceAsStream("/exam.properties")) {
      p.load(is);
    }
    return p.getProperty("brave-karaf.version");
  }

  static void assertPropertyEquals(ServiceReference<?> ref, String key, Object value) {
    List<String> propertyKeys = Arrays.asList(ref.getPropertyKeys());
    if (!propertyKeys.contains(key)) {
      throw new ComparisonFailure("Expected propertyKey", key, propertyKeys.toString());
    }
    assertEquals(value, ref.getProperty(key));
  }
}
