package com.luufery.agent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.ResettableClassFileTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.HashMap;

import static java.lang.Thread.currentThread;

public class MyPlugin {

    private static final String TARGET_CLASS = "com.luufery.rasp.test.SimpleController";

    private static final String TARGET_METHOD = "test";


    public static void load(Instrumentation instrumentation) {
        System.out.println("当前classloader::::" + currentThread().getContextClassLoader().getClass().getName());

        AgentBuilder.Default agentBuilder = new AgentBuilder.Default();
        ResettableClassFileTransformer test = agentBuilder.with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .disableClassFormatChanges()
                .type(ElementMatchers.named(TARGET_CLASS))
                .transform((DynamicType.Builder<?> builder,
                            TypeDescription type,
                            ClassLoader loader,
                            JavaModule module) -> builder.visit(Advice.to(TimeMeasurementAdvice.class).on(ElementMatchers.named(TARGET_METHOD)))).installOn(instrumentation);
        System.out.println("=========");
        //这里直接删除自定义的Transformer,下次transform将不会生效
        instrumentation.removeTransformer(test);
    }

    public static void unload(Instrumentation instrumentation) {
        try {
            instrumentation.retransformClasses(Class.forName(TARGET_CLASS));
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
