package com.luufery.agent.definition;

import com.luufery.agent.advice.Demo31Monitor;
import com.luufery.agent.advice.Demo3Monitor;
import com.luufery.bytebuddy.api.advice.RaspAdvice;
import com.luufery.bytebuddy.api.plugin.spi.AbstractPluginDefinitionService;
import com.luufery.bytebuddy.api.spi.definition.PluginDefinitionService;
import net.bytebuddy.matcher.ElementMatchers;
import org.kohsuke.MetaInfServices;

import static net.bytebuddy.matcher.ElementMatchers.named;

@MetaInfServices(PluginDefinitionService.class)
public class DemoPluginDefinitionService extends AbstractPluginDefinitionService {

    private static final Class<? extends RaspAdvice> SCHEMA_METADATA_LOADER_ADVICE_CLASS = Demo3Monitor.class;

    @Override
    public void defineInterceptors() {
        defineInterceptor("jakarta.servlet.http.HttpServlet", "javax.servlet.http.HttpServlet")
                .on(named("doGet").or(named("doPost")))
                .implement(SCHEMA_METADATA_LOADER_ADVICE_CLASS)
                .build()
        ;
        defineInterceptor("com.luufery.servlet.Utils")
                .on(named("test"))
                .implement(Demo31Monitor.class)
                .build()
        ;
    }

    @Override
    public String getType() {
        return "demo";
    }


    public String[] preLoadClass() {
        return new String[]{"jakarta.servlet.http.HttpServlet", "javax.servlet.http.HttpServlet"};
    }
}
