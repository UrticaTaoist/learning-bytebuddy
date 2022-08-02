package com.luufery.bytebuddy.core.module;

import com.luufery.bytebuddy.api.module.CoreModule;
import com.luufery.bytebuddy.api.module.CoreModuleManager;
import com.luufery.bytebuddy.api.spi.definition.PluginDefinitionService;
import com.luufery.bytebuddy.core.classloader.ModuleJarClassLoader;
import com.luufery.bytebuddy.core.socket.SocketServer;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.*;

import static com.luufery.bytebuddy.core.socket.SocketServer.instrumentation;


public class ModuleJarLoader {

    private static ModuleJarLoader loader;

    public static ModuleJarLoader getInstance() {
        if (loader == null) {
            synchronized (ModuleJarLoader.class) {
                if (loader == null) {
                    return new ModuleJarLoader();
                }
            }
        }
        return loader;
    }

    public void loadModule(String name) throws IOException {
        System.out.println("name::" + name);
        File file = new File("/Users/luufery/workspace/com/luufery/learning-bytebuddy/simple-modules/" + name + "/target/" + name + "-1.0-SNAPSHOT-jar-with-dependencies.jar");
        ModuleJarClassLoader moduleJarClassLoader = new ModuleJarClassLoader(file);
        ServiceLoader<PluginDefinitionService> services = ServiceLoader.load(PluginDefinitionService.class, moduleJarClassLoader);
        for (PluginDefinitionService service : services) {
            Collection<CoreModule> modules = service.load(moduleJarClassLoader);
            System.out.println("transformer::" + modules);
            for (CoreModule coreModule : modules) {
                instrumentation.addTransformer(coreModule.getTransformer(), true);
                try {
                    instrumentation.retransformClasses(Class.forName(coreModule.getTargetClass()));
                    instrumentation.removeTransformer(coreModule.getTransformer());
                } catch (UnmodifiableClassException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
            moduleJarClassLoader.closeIfPossible();
//            System.out.println(transformer.getClass().getName());
        }
//        try {
//            System.out.println(this.getClass().getClassLoader().getClass().getName());
//            //这里的思考是这样的, 一个切面可以有多个切点, 但我们的最小操作单位应当是一个切面. 不可以单独加载或卸载某个切点.
//            Map<String, PluginInterceptorPoint> pointMap = loadAllPlugins(this.getClass().getClassLoader());
//            for (PluginInterceptorPoint value : pointMap.values()) {
//                CoreModuleManager moduleManager = SocketServer.getCoreModuleManager();
//                CoreModule coreModule = new CoreModule();
//                coreModule.setModuleJar(file);
//                coreModule.setTargetClass(value.getTargetClass());
//                coreModule.setClassLoader(this.getClass().getClassLoader());
//                coreModule.setPoint(value);
//                moduleManager.active(coreModule);
//            }
//
//        } finally {
////            if (null != moduleJarClassLoader)
////                moduleJarClassLoader.closeIfPossible();
//
//        }


//        pointMap.forEach((k, v) -> {
//            System.out.println(k + ":" + v.getTargetClass());
//            for (RaspTransformationPoint<? extends RaspAdvice> raspTransformationPoint : v.getTransformationPoint()) {
//                CoreModule coreModule = new CoreModule();
//                coreModule.setModuleJar(file);
//                coreModule.setClassLoader(moduleJarClassLoader);
//                coreModule.setPoint(raspTransformationPoint);
//            }
//        });


//        try {
//
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }


//        AgentBuilder.Default agentBuilder = new AgentBuilder.Default();


        //这里没有做任何缓存,剩下的之后再写吧.
//        for (PluginInterceptorPoint value : SpiPluginLauncher.interceptorPointMap.values()) {
//            for (RaspTransformationPoint<?> raspTransformationPoint : value.getTransformationPoint()) {
//                System.out.println("advice::::" + raspTransformationPoint.getClassOfAdvice());
//                agentBuilder.with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
//                        .disableClassFormatChanges()
//                        .type(ElementMatchers.named(value.getTargetClass()))
//                        .transform((DynamicType.Builder<?> builder,
//                                    TypeDescription type,
//                                    ClassLoader loader,
//                                    JavaModule module) -> builder.visit(
//                                Advice.to(raspTransformationPoint.getClassOfAdvice())
//                                        .on(raspTransformationPoint.getMatcher()))).installOn(instrumentation);
//            }
//        }


    }


}
