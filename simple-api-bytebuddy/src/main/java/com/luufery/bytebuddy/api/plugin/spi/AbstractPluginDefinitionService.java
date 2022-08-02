package com.luufery.bytebuddy.api.plugin.spi;


import com.luufery.bytebuddy.api.module.ModuleJar;
import com.luufery.bytebuddy.api.Spy;
import com.luufery.bytebuddy.api.advice.RaspAdvice;
import com.luufery.bytebuddy.api.module.CoreModule;
import com.luufery.bytebuddy.api.plugin.spy.SpyAdvice;
import com.luufery.bytebuddy.api.plugin.point.PluginInterceptorPoint;
import com.luufery.bytebuddy.api.plugin.point.RaspTransformationPoint;
import com.luufery.bytebuddy.api.spi.definition.PluginDefinitionService;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.util.*;
import java.util.stream.Collectors;

import static com.luufery.bytebuddy.api.plugin.spi.SpiPluginLauncher.loadAllPlugins;

@Slf4j
public abstract class AbstractPluginDefinitionService implements PluginDefinitionService {

    private final ByteBuddy byteBuddy = new ByteBuddy();

    private final AgentBuilder.Default agentBuilder = new AgentBuilder.Default();

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
    final public Collection<CoreModule> load(ModuleJar moduleJar) {
        List<CoreModule> coreModules = new ArrayList<>();
        try {
            for (PluginInterceptorPoint value : loadAllPlugins(moduleJar.getClassLoader()).values()) {
                for (RaspTransformationPoint<?> raspTransformationPoint : value.getTransformationPoint()) {
                    log.debug("advice loading::{}", raspTransformationPoint.getClassOfAdvice());

                    ClassFileTransformer classFileTransformer = agentBuilder.with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                            .disableClassFormatChanges()
                            .type(ElementMatchers.named(value.getTargetClass()))
                            .transform((DynamicType.Builder<?> builder,
                                        TypeDescription type,
                                        ClassLoader loader,
                                        JavaModule module) -> builder.visit(
                                    Advice.to(loadSpy(raspTransformationPoint.getClassOfAdvice(), SpyAdvice.class))
                                            .on(raspTransformationPoint.getMatcher())))

                            .makeRaw();
                    coreModules.add(CoreModule.builder()
                            .moduleJar(moduleJar.getModuleJarFile())
                            .classLoader(moduleJar.getClassLoader())
                            .targetClass(value.getTargetClass())
                            .transformer(classFileTransformer).build()
                    );
                }
            }
        } catch (IOException e) {
            log.warn("CoreModule加载失败", e);
        }
        return coreModules;

    }


    public Class<? extends RaspAdvice> loadSpy(Class<? extends RaspAdvice> source, Class<? extends Spy> spy) {
        return byteBuddy.redefine(source).visit(Advice.to(spy)
                        .on(ElementMatchers.isAnnotatedWith(Advice.OnMethodEnter.class)
                                .or(ElementMatchers.isAnnotatedWith(Advice.OnMethodExit.class))))
                .make()
                .load(source.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST_PERSISTENT).getLoaded();
    }
}
