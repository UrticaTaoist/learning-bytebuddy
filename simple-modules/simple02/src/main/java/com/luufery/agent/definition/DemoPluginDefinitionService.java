package com.luufery.agent.definition;

import com.luufery.agent.advice.DemoMonitor;
import com.luufery.bytebuddy.api.advice.RaspAdvice;
import com.luufery.bytebuddy.api.plugin.AbstractPluginDefinitionService;
import com.luufery.bytebuddy.api.spi.definition.PluginDefinitionService;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.matcher.ElementMatchers;
import org.kohsuke.MetaInfServices;

import java.lang.instrument.ClassFileTransformer;

@MetaInfServices(PluginDefinitionService.class)
public class DemoPluginDefinitionService extends AbstractPluginDefinitionService {
    private static final String SCHEMA_METADATA_LOADER_CLASS = "com.luufery.rasp.test.SimpleController";

    private static final String SCHEMA_METADATA_LOADER_METHOD_NAME = "test";

    private static final Class<? extends RaspAdvice> SCHEMA_METADATA_LOADER_ADVICE_CLASS = DemoMonitor.class;

    @Override
    public void defineInterceptors() {
        System.out.println("被加载了啊");
        defineInterceptor(SCHEMA_METADATA_LOADER_CLASS)
                .on(ElementMatchers.named(SCHEMA_METADATA_LOADER_METHOD_NAME))
                .implement(SCHEMA_METADATA_LOADER_ADVICE_CLASS)
                .build()
        ;
    }

    @Override
    public String getType() {
        return "demo";
    }


}
