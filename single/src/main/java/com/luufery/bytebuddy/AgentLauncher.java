package com.luufery.bytebuddy;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.UnmodifiableClassException;
import java.util.Collections;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class AgentLauncher {

    public static void install() {
        try {
            ElementMatcher.Junction<TypeDescription> subTypeOf = null;

            AgentBuilder agentBuilder = new AgentBuilder.Default(new ByteBuddy());
//            ClassFileTransformer transformer =
            Class<?>[] allLoadedClasses = Bootstrap.instrumentation.getAllLoadedClasses();
            System.out.println("????");
            agentBuilder = agentBuilder
                    .with(new AgentListener())
//                    .with(AgentBuilder.InjectionStrategy.Disabled.INSTANCE)
                    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
//                    .with(AgentBuilder.Listener.StreamWriting.toSystemOut())
                    .ignore(none())
                    .disableClassFormatChanges();

//            agentBuilder = agentBuilder.type(ElementMatchers.named("java.nio.file.Files"))
//
//                    .transform(new AgentBuilder.Transformer() {
//                        @Override
//                        public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
//                            return builder.visit(Advice.to(DemoMonitor.class).on(ElementMatchers.named("newOutputStream")));
//                        }
//                    })
//            ;
//            agentBuilder = agentBuilder.type(ElementMatchers.named("com.luufery.rasp.test.SimpleController"))
//                    .transform(new AgentBuilder.Transformer() {
//                        @Override
//                        public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
//                            return builder.visit(Advice.to(DemoMonitor.class).on(ElementMatchers.named("test")));
//                        }
//                    })
//
//            ;

            agentBuilder = agentBuilder.type(typeMatch(allLoadedClasses, new String[]{"jakarta.servlet.http.HttpServlet","javax.servlet.http.HttpServlet"}))
                    .transform(new AgentBuilder.Transformer() {
                        @Override
                        public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
                            return builder.visit(Advice.to(DemoMonitor.class).on(named("doGet").or(named("doPost")).or(named("service")).or(named("_jspService"))));
                        }
                    })
            ;
            agentBuilder.installOn(Bootstrap.instrumentation);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
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


    private static Class<?> load(String name) {
        try {
            return Class.forName(name);
        } catch (Exception e) {
            return null;
        }
    }

}
