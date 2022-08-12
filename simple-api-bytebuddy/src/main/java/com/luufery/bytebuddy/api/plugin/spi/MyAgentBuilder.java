package com.luufery.bytebuddy.api.plugin.spi;

import com.luufery.bytebuddy.api.Spy;
import com.luufery.bytebuddy.api.advice.RaspAdvice;
import com.luufery.bytebuddy.api.plugin.listener.AgentListener;
import com.luufery.bytebuddy.api.plugin.point.RaspTransformationPoint;
import com.luufery.bytebuddy.api.plugin.spy.SpyAdvice;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.AgentBuilder.*;
import net.bytebuddy.agent.builder.AgentBuilder.Identified.*;
import net.bytebuddy.agent.builder.CustomAgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.lang.instrument.ClassFileTransformer;
import java.util.Collections;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class MyAgentBuilder {

    private AgentBuilder builder;

    public MyAgentBuilder(String module) {
        this.builder = new CustomAgentBuilder(module)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(new AgentListener())
                .disableClassFormatChanges()
//                .ignore(ElementMatchers.none())
        ;

    }

    public synchronized MyAgentBuilder attach(ElementMatcher.Junction<TypeDescription> elementMatcher, List<RaspTransformationPoint<? extends RaspAdvice>> raspTransformationPoints) {
        Extendable transform = null;
        Narrowable narrowable = builder.type(elementMatcher);
        for (RaspTransformationPoint<?> raspTransformationPoint : raspTransformationPoints) {
            if (transform == null) {
                transform = narrowable.transform(parseTransformer(raspTransformationPoint));
            } else {
                transform = transform.transform(parseTransformer(raspTransformationPoint));
            }
        }
        builder = transform;
        return this;
    }

    private AgentBuilder.Transformer parseTransformer(final RaspTransformationPoint<?> raspTransformationPoint) {
        return new Transformer() {
            @Override
            public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
                return builder.visit(
                        Advice.to(loadSpy(raspTransformationPoint.getClassOfAdvice(), SpyAdvice.class))
                                .on(raspTransformationPoint.getMatcher()));
            }
        };
    }

    public AgentBuilder entity() {
        return builder;
    }

    public ClassFileTransformer makeRaw() {
        return this.builder.makeRaw();
    }


    public static ElementMatcher.Junction<TypeDescription> typeMatch(Class<?>[] allLoadedClasses, String[] names) {
        ElementMatcher.Junction<TypeDescription> elementMatcher = ElementMatchers.none();

        //TODO 这里是过程化的,应该抽象
        for (String name : names) {
            for (Class<?> loaded : allLoadedClasses) {
                ElementMatcher.Junction<TypeDescription> subsTypeOf = isSubsTypeOf(loaded, name);
                if (subsTypeOf != null && !subsTypeOf.equals(ElementMatchers.none())) {
                    elementMatcher = elementMatcher.or(subsTypeOf);
                }
                ElementMatcher.Junction<TypeDescription> supersTypeOf = isSupersTypeOf(loaded, name);
                if (supersTypeOf != null && !supersTypeOf.equals(ElementMatchers.none())) {
                    elementMatcher = elementMatcher.or(supersTypeOf);
                }

            }
            elementMatcher = elementMatcher.or(ElementMatchers.named(name));

        }
        return elementMatcher;
    }


    public static ElementMatcher.Junction<TypeDescription> isSubsTypeOf(Class<?> loadedClass, String clazzName) {
        ElementMatcher.Junction<TypeDescription> subTypeOf = ElementMatchers.none();
        if (loadedClass.getName().equals(clazzName)) {
            subTypeOf = subTypeOf.or(ElementMatchers.isSubTypeOf(loadedClass));
        }
        return subTypeOf;
    }

    public static ElementMatcher.Junction<TypeDescription> isSupersTypeOf(Class<?> loadedClass, String clazzName) {
        ElementMatcher.Junction<TypeDescription> subTypeOf = ElementMatchers.none();
        if (loadedClass.getName().equals(clazzName)) {
            subTypeOf = subTypeOf.or(ElementMatchers.isSuperTypeOf(loadedClass));
        }
        return subTypeOf;
    }


    public Class<? extends RaspAdvice> loadSpy(Class<? extends RaspAdvice> source, Class<? extends Spy> spy) {
        return new ByteBuddy().redefine(source).visit(Advice.to(spy)
                        .on(ElementMatchers.isAnnotatedWith(Advice.OnMethodEnter.class)
                                .or(ElementMatchers.isAnnotatedWith(Advice.OnMethodExit.class))))
                .make()
                .load(source.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST_PERSISTENT).getLoaded();
    }
}
