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

package io.zipkin.brave.itests;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;

import java.util.ArrayList;
import java.util.List;

import com.github.kristofa.brave.Brave;
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
import zipkin.Span;
import zipkin.reporter.Reporter;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class BraveTest {
    List<Object> spans = new ArrayList<>();

    @Configuration
    public static Option[] configuration() throws Exception {
        MavenArtifactUrlReference karaf = maven().groupId("org.apache.karaf").artifactId("apache-karaf")
            .type("zip").version("4.0.7");
        MavenUrlReference brave = maven().groupId("net.lr.brave.osgi").artifactId("brave-features").type("xml")
           .classifier("features").version("0.0.1-SNAPSHOT");
        return new Option[] //
        {
         karafDistributionConfiguration().frameworkUrl(karaf).useDeployFolder(false),
         configureConsole().ignoreLocalConsole(), //
         logLevel(LogLevel.INFO), //
         keepRuntimeFolder(), //
         features(brave, "brave-core")
        };
    }

    @Test
    public void shouldHaveBundleContext() {
        Reporter<Span> local = new Reporter<Span>() {

            @Override
            public void report(Span span) {
                spans.add(span);
            }
        };
        Brave brave = new Brave.Builder().reporter(local).build();
        brave.localTracer().startNewSpan("test", "testop");
        brave.localTracer().finishSpan();
        Assert.assertThat(1, equalTo(spans.size()));
    }

}
