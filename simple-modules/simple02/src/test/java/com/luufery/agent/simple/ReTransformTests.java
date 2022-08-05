package com.luufery.agent.simple;

import com.luufery.agent.simple.dynamic.HotReloadWorker;
import com.luufery.bytebuddy.api.plugin.spy.SpyAdvice;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;
import org.junit.Test;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class ReTransformTests {

    @Test
    public void test01() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, UnmodifiableClassException, MalformedURLException {

        MyClassloader classLoader01 = new MyClassloader("/Users/luufery/workspace/com/luufery/ClassLoaderDemo/test01.jar");
        Class<?> class01 = classLoader01.loadClass("org.example.Foo");
        class01.getDeclaredMethod("hello").invoke(null);

        MyClassloader classLoader02 = new MyClassloader("/Users/luufery/workspace/com/luufery/ClassLoaderDemo/test02.jar");

        Class<?> class02 = classLoader02.loadClass("org.example.Foo");
        class02.getDeclaredMethod("hello").invoke(null);

        Instrumentation inst = ByteBuddyAgent.install();


        HotReloadWorker.doReload(inst,
                "/Users/luufery/workspace/com/luufery/ClassLoaderDemo/src/main/java/org/example/Foo.java");


        System.out.println("===========================");
        classLoader01.loadClass("org.example.Foo").getDeclaredMethod("hello").invoke(null);
        System.out.println("===========================");
        classLoader02.loadClass("org.example.Foo").getDeclaredMethod("hello").invoke(null);


    }


}
