package com.luufery.bytebuddy.api.plugin.spi;


import com.luufery.bytebuddy.api.ModuleJar;
import com.luufery.bytebuddy.api.module.CoreModule;
import com.luufery.bytebuddy.api.plugin.point.PluginInterceptorPoint;
import com.luufery.bytebuddy.api.plugin.point.RaspTransformationPoint;
import com.luufery.bytebuddy.api.spi.definition.PluginDefinitionService;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.util.*;
import java.util.stream.Collectors;

import static com.luufery.bytebuddy.api.plugin.spi.SpiPluginLauncher.loadAllPlugins;

public abstract class AbstractPluginDefinitionService implements PluginDefinitionService {

    private final Map<String, PluginInterceptorPoint.Builder> interceptorPointMap = new HashMap<>();

    /**
     * 自定义拦截器处理拦截点
     */
    public abstract void defineInterceptors();

    /**
     * 初始化拦截点集合
     *
     * @return 插件拦截点集合
     */
    public final Collection<PluginInterceptorPoint> install() {
        defineInterceptors();
        return interceptorPointMap.values().stream().map(PluginInterceptorPoint.Builder::install).collect(Collectors.toList());
    }

    /**
     * 自定义拦截器,不在上面使用,而是在每个插件中使用到
     *
     * @param classNameOfTarget class
     * @return 拦截点构建器
     */
    protected final PluginInterceptorPoint.Builder defineInterceptor(final String classNameOfTarget) {
        if (interceptorPointMap.containsKey(classNameOfTarget)) {
            return interceptorPointMap.get(classNameOfTarget);
        }
        PluginInterceptorPoint.Builder builder = PluginInterceptorPoint.intercept(classNameOfTarget);
        interceptorPointMap.put(classNameOfTarget, builder);
        return builder;
    }

    @Override
    public Collection<CoreModule> load(ModuleJar moduleJar) throws IOException {
        Map<String, PluginInterceptorPoint> stringPluginInterceptorPointMap = loadAllPlugins(moduleJar.getClassLoader());
        AgentBuilder.Default agentBuilder = new AgentBuilder.Default();
//        defineInterceptors();
//        System.out.println("interceptorPointMap.size()::" + interceptorPointMap.size());
        List<CoreModule> coreModules = new ArrayList<>();
        System.out.println("stringPluginInterceptorPointMap.size()::" + stringPluginInterceptorPointMap.size());
        for (PluginInterceptorPoint value : stringPluginInterceptorPointMap.values()) {
            for (RaspTransformationPoint<?> raspTransformationPoint : value.getTransformationPoint()) {
                System.out.println("advice::::" + raspTransformationPoint.getClassOfAdvice());
                CoreModule coreModule = new CoreModule();
                ClassFileTransformer classFileTransformer = agentBuilder.with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                        .disableClassFormatChanges()
                        .type(ElementMatchers.named(value.getTargetClass()))
                        .transform((DynamicType.Builder<?> builder,
                                    TypeDescription type,
                                    ClassLoader loader,
                                    JavaModule module) -> builder.visit(
                                Advice.to(raspTransformationPoint.getClassOfAdvice())
                                        .on(raspTransformationPoint.getMatcher())))

                        .makeRaw();
                coreModule.setClassLoader(moduleJar.getClassLoader());
                coreModule.setModuleJar(moduleJar.getModuleJarFile());
                coreModule.setTransformer(classFileTransformer);
                coreModule.setTargetClass(value.getTargetClass());
                coreModules.add(coreModule);
            }
        }
        return coreModules;

    }
}
