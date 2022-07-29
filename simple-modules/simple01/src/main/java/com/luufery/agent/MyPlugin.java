package com.luufery.agent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.ResettableClassFileTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Modifier;
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
                //InstallationListener可以用在测试阶段,也可以在产品中用于事件回调
                .with(new MyInstallationListener())
                .type(ElementMatchers.named(TARGET_CLASS))
                .transform((
                        DynamicType.Builder<?> builder,
                        TypeDescription type,
                        ClassLoader loader,
                        JavaModule module) -> {
//                                        builder.defineMethod("moon",//定义方法的名称
//                                    String.class,//方法的返回值
//                                    Modifier.PUBLIC)//public修饰
//                            .withParameters(String.class)
//                            .intercept(FixedValue.value("sdfsdf"))
//                            .make();
                    return builder.visit(
                            Advice.to(TimeMeasurementAdvice.class)
                                    .on(ElementMatchers.named(TARGET_METHOD)));
                })

                .installOn(instrumentation);
        System.out.println("=========");
        //这里直接删除自定义的Transformer,下次transform将不会生效
        //在实际开发中,应当将Transformer缓存起来,待需要卸载时再删除.
        System.out.println(instrumentation.removeTransformer(test));
    }

    public static void unload(Instrumentation instrumentation) {
        try {
            instrumentation.retransformClasses(Class.forName(TARGET_CLASS));
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
