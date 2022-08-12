package com.luufery.agent.definition;

import com.luufery.agent.advice.Demo2Monitor;
import com.luufery.bytebuddy.api.advice.RaspAdvice;
import com.luufery.bytebuddy.api.plugin.spi.AbstractPluginDefinitionService;
import com.luufery.bytebuddy.api.spi.definition.PluginDefinitionService;
import net.bytebuddy.matcher.ElementMatchers;
import org.kohsuke.MetaInfServices;

import static net.bytebuddy.matcher.ElementMatchers.named;

@MetaInfServices(PluginDefinitionService.class)
public class DemoPluginDefinitionService extends AbstractPluginDefinitionService {

    private static final Class<? extends RaspAdvice> SCHEMA_METADATA_LOADER_ADVICE_CLASS = Demo2Monitor.class;

    @Override
    public void defineInterceptors() {
        defineInterceptor("jakarta.servlet.http.HttpServlet", "javax.servlet.http.HttpServlet")
                .on(named("doGet").or(named("doPost")))
                .implement(SCHEMA_METADATA_LOADER_ADVICE_CLASS)
                .build();
    }

    @Override
    public String getType() {
        return "demo";
    }


}
