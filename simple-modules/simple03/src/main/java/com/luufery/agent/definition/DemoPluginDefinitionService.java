package com.luufery.agent.definition;

import com.luufery.agent.advice.Demo2Monitor;
import com.luufery.bytebuddy.api.advice.RaspAdvice;
import com.luufery.bytebuddy.api.plugin.spi.AbstractPluginDefinitionService;
import com.luufery.bytebuddy.api.spi.definition.PluginDefinitionService;
import net.bytebuddy.matcher.ElementMatchers;
import org.kohsuke.MetaInfServices;

@MetaInfServices(PluginDefinitionService.class)
public class DemoPluginDefinitionService extends AbstractPluginDefinitionService {
    private static final String SCHEMA_METADATA_LOADER_CLASS = "com.luufery.rasp.test.SimpleController";

    private static final String SCHEMA_METADATA_LOADER_METHOD_NAME = "test";

    private static final Class<? extends RaspAdvice> SCHEMA_METADATA_LOADER_ADVICE_CLASS = Demo2Monitor.class;

    @Override
    public void defineInterceptors() {
        defineInterceptor(new String[]{SCHEMA_METADATA_LOADER_CLASS})
                .on(ElementMatchers.named(SCHEMA_METADATA_LOADER_METHOD_NAME))
                .implement(SCHEMA_METADATA_LOADER_ADVICE_CLASS)
                .build()
        ;
//        defineInterceptor("com.luufery.rasp.test.TestClass")
//                .on(MethodDescription::isConstructor)
//                .implement(ConstructorMonitor.class)
//                .build()
//        ;
    }

    @Override
    public String getType() {
        return "demo";
    }


}
