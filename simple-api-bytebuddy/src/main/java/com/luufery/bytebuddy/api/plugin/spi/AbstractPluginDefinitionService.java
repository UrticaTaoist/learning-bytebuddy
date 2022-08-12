package com.luufery.bytebuddy.api.plugin.spi;


import com.luufery.bytebuddy.api.module.ModuleJar;
import com.luufery.bytebuddy.api.Spy;
import com.luufery.bytebuddy.api.advice.RaspAdvice;
import com.luufery.bytebuddy.api.module.CoreModule;
import com.luufery.bytebuddy.api.module.RedefinitionHolder;
import com.luufery.bytebuddy.api.plugin.spy.SpyAdvice;
import com.luufery.bytebuddy.api.plugin.point.PluginInterceptorPoint;
import com.luufery.bytebuddy.api.plugin.point.RaspTransformationPoint;
import com.luufery.bytebuddy.api.spi.definition.PluginDefinitionService;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.ResettableClassFileTransformer;
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

import static com.luufery.bytebuddy.api.plugin.spi.MyAgentBuilder.typeMatch;
import static com.luufery.bytebuddy.api.plugin.util.TypeUtils.*;

//import static com.luufery.bytebuddy.api.plugin.spi.SpiPluginLauncher.*;

@Slf4j
public abstract class AbstractPluginDefinitionService implements PluginDefinitionService {


//    private final Map<String, PluginInterceptorPoint.Builder> interceptorPointMap = new HashMap<>();

//    private final List<String> targetClass = new ArrayList<>();

    private final Set<PluginInterceptorPoint.Builder> interceptorPoints = new HashSet<PluginInterceptorPoint.Builder>();

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
            List<PluginInterceptorPoint> points = new ArrayList<PluginInterceptorPoint>();
            for (PluginInterceptorPoint.Builder interceptorPoint : interceptorPoints) {
                points.add(interceptorPoint.install());
            }
            return points;
//            return interceptorPoints.stream().map(PluginInterceptorPoint.Builder::install).collect(Collectors.toList());
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
    public final PluginInterceptorPoint.Builder defineInterceptor(final String... classNameOfTarget) {
        PluginInterceptorPoint.Builder builder = PluginInterceptorPoint.intercept(classNameOfTarget);

//        this.targetClass.addAll(Arrays.asList(classNameOfTarget));
        interceptorPoints.add(builder);
        return builder;
    }

    public final void undefineInterceptor() {
//        interceptorPointMap.remove(classNameOfTarget);
        interceptorPoints.clear();
    }

    @Override
    final public CoreModule load(Instrumentation instrumentation) {


        Collection<PluginInterceptorPoint> points = this.install();
        Class<?>[] allLoadedClasses = instrumentation.getAllLoadedClasses();


        MyAgentBuilder myAgentBuilder = new MyAgentBuilder(this.getType());

        for (PluginInterceptorPoint point : points) {

            ElementMatcher.Junction<TypeDescription> elementMatcher = typeMatch(allLoadedClasses, point.getTargetClass());

            myAgentBuilder.attach(elementMatcher, point.getTransformationPoint()).entity();

            point.getTransformationPoint().clear();

        }
        points.clear();

        ResettableClassFileTransformer transformer = myAgentBuilder.entity().installOn(instrumentation);

        try {

            return CoreModule.builder()
                    .targetClass(RedefinitionHolder.getLoadedClass(this.getType()).toArray(new Class[0]))
                    .name(this.getType())
                    .transformer(transformer).build();
        } finally {
            RedefinitionHolder.clear(this.getType());
        }

    }


    public Class<? extends RaspAdvice> loadSpy(Class<? extends RaspAdvice> source, Class<? extends Spy> spy) {
        return new ByteBuddy().redefine(source).visit(Advice.to(spy)
                        .on(ElementMatchers.isAnnotatedWith(Advice.OnMethodEnter.class)
                                .or(ElementMatchers.isAnnotatedWith(Advice.OnMethodExit.class))))
                .make()
                .load(source.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST_PERSISTENT).getLoaded();
    }
}
