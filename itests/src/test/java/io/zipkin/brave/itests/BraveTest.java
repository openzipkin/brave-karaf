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
package io.zipkin.brave.itests;

import java.util.Collection;
import javax.inject.Inject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import zipkin2.reporter.Sender;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.cm.ConfigurationAdminOptions.newConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class BraveTest {
  private static final String FILTER_KAFKA = "(component.name=io.zipkin.sender.kafka)";
  private static final String FILTER_OKHTTP = "(component.name=io.zipkin.sender.okhttp)";
  private static final String FILTER_URLCONNECTION =
      "(component.name=io.zipkin.sender.urlconnection)";

  @Inject
  BundleContext context;

  @Configuration
  public static Option[] configuration() {
    MavenArtifactUrlReference karaf = maven().groupId("org.apache.karaf").artifactId("apache-karaf")
        .type("zip").version("4.2.0");
    MavenUrlReference brave =
        maven().groupId("io.zipkin.brave.karaf").artifactId("brave-features").type("xml")
            .classifier("features").version("0.1.0-SNAPSHOT");
    return new Option[] {
        karafDistributionConfiguration().frameworkUrl(karaf).useDeployFolder(false),
        configureConsole().ignoreLocalConsole(),
        logLevel(LogLevel.INFO),
        keepRuntimeFolder(),
        features(brave, "brave", "brave-kafka", "brave-okhttp"),
        // Create an empty config to trigger creation of component
        newConfiguration("io.zipkin.sender.urlconnection").asOption(),
        newConfiguration("io.zipkin.sender.okhttp").asOption(),
        newConfiguration("io.zipkin.sender.kafka").asOption()
    };
  }

  @Test
  public void checkSenderUrlConnection() throws InvalidSyntaxException {
    ServiceReference<Sender> ref = getSingleSender(FILTER_URLCONNECTION);
    Assert.assertEquals(10000, ref.getProperty("connectTimeout"));
  }

  @Test
  public void checkSenderKafka() throws InvalidSyntaxException {
    ServiceReference<Sender> ref = getSingleSender(FILTER_KAFKA);
    Assert.assertEquals("zipkin", ref.getProperty("topic"));
  }

  @Test
  public void checkSenderOkHttp() throws InvalidSyntaxException {
    ServiceReference<Sender> ref = getSingleSender(FILTER_OKHTTP);
    Assert.assertEquals("zipkin", ref.getProperty("topic"));
  }

  @SuppressWarnings("rawtypes")
  private ServiceReference<Sender> getSingleSender(String filter) throws InvalidSyntaxException {
    Collection<ServiceReference<Sender>> refs = context.getServiceReferences(Sender.class, filter);
    Assert.assertEquals(1, refs.size());
    return refs.iterator().next();
  }
}
