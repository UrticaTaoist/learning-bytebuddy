package com.luufery.bytebuddy.core.module;

import com.luufery.bytebuddy.api.advice.RaspAdvice;
import com.luufery.bytebuddy.api.point.PluginInterceptorPoint;
import com.luufery.bytebuddy.api.point.RaspTransformationPoint;
import com.luufery.bytebuddy.api.spi.definition.PluginDefinitionService;
import com.luufery.bytebuddy.core.classloader.ModuleJarClassLoader;
import com.luufery.bytebuddy.core.socket.SocketServer;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.ResettableClassFileTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ServiceLoader;

import static com.luufery.bytebuddy.core.module.SpiPluginLauncher.loadAllPlugins;
import static com.luufery.bytebuddy.core.socket.SocketServer.instrumentation;

public class ModuleJarLoader {

    public void loadModule(String name) throws IOException {

        File file = new File("/Users/luufery/workspace/com/luufery/learning-bytebuddy/simple-modules/" + name + "/target/" + name + "-1.0-SNAPSHOT-jar-with-dependencies.jar");
        ModuleJarClassLoader moduleJarClassLoader = new ModuleJarClassLoader(file);
//        ServiceLoader<PluginDefinitionService> raspAdvices = ServiceLoader.load(PluginDefinitionService.class, moduleJarClassLoader);

        try {
            loadAllPlugins(moduleJarClassLoader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        AgentBuilder.Default agentBuilder = new AgentBuilder.Default();

        //这里没有做任何缓存,剩下的之后再写吧.
        for (PluginInterceptorPoint value : SpiPluginLauncher.interceptorPointMap.values()) {
            for (RaspTransformationPoint<?> raspTransformationPoint : value.getTransformationPoint()) {
                System.out.println("advice::::" + raspTransformationPoint.getClassOfAdvice());
                agentBuilder.with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                        .disableClassFormatChanges()
                        .type(ElementMatchers.named(value.getTargetClass()))
                        .transform((DynamicType.Builder<?> builder,
                                    TypeDescription type,
                                    ClassLoader loader,
                                    JavaModule module) -> builder.visit(
                                Advice.to(raspTransformationPoint.getClassOfAdvice())
                                        .on(raspTransformationPoint.getMatcher()))).installOn(instrumentation);
            }
        }


    }


}
