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
package io.zipkin.brave.osgi.exporter;

import java.util.Hashtable;
import java.util.Map;

import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.Sampler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import zipkin.Span;
import zipkin.reporter.Reporter;

@Component //
( //
    immediate = true, //
    name = "io.zipkin.brave" //
)
@Designate(ocd = BraveExporter.Config.class)
public class BraveExporter {
    @Reference
    Reporter<Span> reporter;

    private ServiceRegistration<Brave> reg;

    @ObjectClassDefinition(name = "Brave")
    @interface Config {
        String name() default "default";
        boolean traceId128Bit() default false;
        float rate() default 1;
    }

    @Activate
    public void activate(Config config, BundleContext context, Map<String, String> properties) {
        Brave brave = new Brave.Builder(config.name())
            .reporter(reporter) //
            .traceId128Bit(config.traceId128Bit()) //
            .traceSampler(Sampler.create(config.rate()))
            .build();
        reg = context.registerService(Brave.class, brave, new Hashtable<String, String>(properties));
    }
    
    @Deactivate
    public void deactive() {
        reg.unregister();
    }

}
