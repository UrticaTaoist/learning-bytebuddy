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
            for (Class<?> clazz : Bootstrap.instrumentation.getAllLoadedClasses()) {
                if (clazz.getName().contains("Files")) {
                    System.out.println("找到啦");
                }
            }
            Class<?> aClass;
            Class<?> bClass;

            aClass = Class.forName("java.nio.file.Files");
            bClass = Class.forName("java.nio.file.Files");
            ElementMatcher.Junction<TypeDescription> subTypeOf = null;

            System.out.println(aClass.getName());
            AgentBuilder agentBuilder = new AgentBuilder.Default(new ByteBuddy());
//            ClassFileTransformer transformer =
            agentBuilder = agentBuilder
                    .with(new AgentListener())
                    .with(AgentBuilder.InjectionStrategy.Disabled.INSTANCE)
                    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
//                    .with(AgentBuilder.Listener.StreamWriting.toSystemOut())
                    .ignore(none())
                    .disableClassFormatChanges();

            agentBuilder = agentBuilder.type(ElementMatchers.named("java.nio.file.Files"))

                    .transform((builder, typeDescription, classLoader, module)
                            -> builder.visit(Advice.to(DemoMonitor.class).on(ElementMatchers.named("newOutputStream"))))
            ;
            agentBuilder = agentBuilder.type(ElementMatchers.named("com.luufery.rasp.test.SimpleController"))
                    .transform((builder, typeDescription, classLoader, module)
                            -> builder.visit(Advice.to(DemoMonitor.class).on(ElementMatchers.named("test"))))

            ;

            agentBuilder = agentBuilder.type(isSupersTypeOf("jakarta.servlet.http.HttpServlet", "javax.servlet.http.HttpServlet"))
                    .transform((builder, typeDescription, classLoader, module)
                            -> builder.visit(Advice.to(DemoMonitor.class).on(ElementMatchers.named("doGet").or(ElementMatchers.named("doPost")))))
            ;
            agentBuilder.installOn(Bootstrap.instrumentation);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private static ElementMatcher.Junction<TypeDescription> isSupersTypeOf(String... classes) {
        ElementMatcher.Junction<TypeDescription> subTypeOf = ElementMatchers.is(Object.class);
        for (String clazzName : classes) {
            Class<?> load = load(clazzName);
            if (load != null) {
                subTypeOf = subTypeOf.or(ElementMatchers.isSuperTypeOf(load));
            }
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
