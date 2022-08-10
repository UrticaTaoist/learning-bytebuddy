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
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.*;
import java.util.stream.Collectors;

//import static com.luufery.bytebuddy.api.plugin.spi.SpiPluginLauncher.*;

@Slf4j
public abstract class AbstractPluginDefinitionService implements PluginDefinitionService {


//    private final Map<String, PluginInterceptorPoint.Builder> interceptorPointMap = new HashMap<>();

    private ElementMatcher.Junction<TypeDescription> targetClass;

    private final Set<PluginInterceptorPoint.Builder> interceptorPoints = new HashSet<>();

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
//        return interceptorPointMap.values().stream().map(PluginInterceptorPoint.Builder::install).collect(Collectors.toList());
        try {
            return interceptorPoints.stream().map(PluginInterceptorPoint.Builder::install).collect(Collectors.toList());
        } finally {
            interceptorPoints.clear();
        }
    }

    /**
     * 自定义拦截器,不在上面使用,而是在每个插件中使用到
     *
     * @param classNameOfTarget class
     * @return 拦截点构建器
     */
    public final PluginInterceptorPoint.Builder defineInterceptor(final ElementMatcher.Junction<TypeDescription> classNameOfTarget) {
//        if (interceptorPointMap.containsKey(classNameOfTarget)) {
//            return interceptorPointMap.get(classNameOfTarget);
//        }
        PluginInterceptorPoint.Builder builder = PluginInterceptorPoint.intercept(classNameOfTarget);
        //TODO 这里的设计,需要明确只有一个target
        this.targetClass = classNameOfTarget;
//        interceptorPointMap.put(classNameOfTarget, builder);
        interceptorPoints.add(builder);
        return builder;
    }

    public final void undefineInterceptor(final String classNameOfTarget) {
//        interceptorPointMap.remove(classNameOfTarget);
        interceptorPoints.clear();
    }

    @Override
    final public Collection<CoreModule> load(Instrumentation instrumentation) {

        List<CoreModule> coreModules = new ArrayList<>();
        Collection<PluginInterceptorPoint> points = this.install();
        AgentBuilder.Identified.Narrowable narrowable = new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .disableClassFormatChanges()
                .ignore(ElementMatchers.none())
                .type(this.targetClass);
        AgentBuilder.Identified.Extendable transform = null;
        for (PluginInterceptorPoint point : points) {
            for (RaspTransformationPoint<?> raspTransformationPoint : point.getTransformationPoint()) {
                transform = narrowable
                        .transform((DynamicType.Builder<?> builder,
                                    TypeDescription type,
                                    ClassLoader loader,
                                    JavaModule module) -> builder.visit(
                                Advice.to(loadSpy(raspTransformationPoint.getClassOfAdvice(), SpyAdvice.class))
                                        .on(raspTransformationPoint.getMatcher())));
            }
            if (transform != null) {
                List<Class<?>> targetClasses = new ArrayList<>();
                for (Class<?> loadedClass : instrumentation.getAllLoadedClasses()) {
                    if (targetClass.matches(TypeDescription.ForLoadedType.of(loadedClass))) {
                        targetClasses.add(loadedClass);
                    }
                }

                point.getTransformationPoint().clear();
                coreModules.add(CoreModule.builder()
                        .targetClass(targetClasses.toArray(new Class[0]))
                        .transformer(transform.makeRaw()).build());

            }

        }
        points.clear();
        return coreModules;

    }


    public Class<? extends RaspAdvice> loadSpy(Class<? extends RaspAdvice> source, Class<? extends Spy> spy) {
        return new ByteBuddy().redefine(source).visit(Advice.to(spy)
                        .on(ElementMatchers.isAnnotatedWith(Advice.OnMethodEnter.class)
                                .or(ElementMatchers.isAnnotatedWith(Advice.OnMethodExit.class))))
                .make()
                .load(source.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST_PERSISTENT).getLoaded();
    }
}
