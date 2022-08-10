package com.luufery.agent.definition;

import com.luufery.agent.advice.Demo3Monitor;
import com.luufery.bytebuddy.api.advice.RaspAdvice;
import com.luufery.bytebuddy.api.plugin.spi.AbstractPluginDefinitionService;
import com.luufery.bytebuddy.api.spi.definition.PluginDefinitionService;
import net.bytebuddy.matcher.ElementMatchers;
import org.kohsuke.MetaInfServices;

import static com.luufery.bytebuddy.api.plugin.util.TypeUtils.isSupersTypeOf;
import static net.bytebuddy.matcher.ElementMatchers.named;

@MetaInfServices(PluginDefinitionService.class)
public class DemoPluginDefinitionService extends AbstractPluginDefinitionService {

    private static final Class<? extends RaspAdvice> SCHEMA_METADATA_LOADER_ADVICE_CLASS = Demo3Monitor.class;

    @Override
    public void defineInterceptors() {
        defineInterceptor(isSupersTypeOf("jakarta.servlet.http.HttpServlet", "javax.servlet.http.HttpServlet"))

                .on(named("doGet").or(named("doPost")))
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
