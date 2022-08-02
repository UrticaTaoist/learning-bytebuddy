package com.luufery.bytebuddy.api.plugin.spi;

import com.luufery.bytebuddy.api.advice.RaspAdvice;
import com.luufery.bytebuddy.api.plugin.spy.SpyAdvice;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.matcher.ElementMatchers;
import org.junit.Test;

import javax.annotation.Resource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class BytebuddyTests {

    @Test
    public void test01(){
        Class<? extends RaspAdvice> loaded = new ByteBuddy().subclass(DemoMonitor.class)
                .visit(Advice.to(SpyAdvice.class).on(ElementMatchers.any())).make()
                .load(Thread.currentThread().getContextClassLoader()).getLoaded();
        for (Method declaredMethod : loaded.getDeclaredMethods()) {
            System.out.println(declaredMethod);
        }
    }

//    @Test
//    public void test02(){
//        DynamicType.Unloaded<DemoMonitor> dynamicType = new ByteBuddy().subclass(DemoMonitor.class).method(ElementMatchers.any()).intercept(MethodDelegation.to(MonitorDemo.class)).make();
//        Class<? extends DemoMonitor> loaded = dynamicType.load(DemoMonitor.class.getClassLoader()).getLoaded();
//        for (Method declaredMethod : loaded.getDeclaredMethods()) {
//            System.out.println(declaredMethod);
//        }
//
//    }

    @Test
    public void test03(){
        Class<? extends DemoMonitor> loaded = new ByteBuddy().redefine(DemoMonitor.class).visit(Advice.to(SpyAdvice.class).on(ElementMatchers.any())).make()
                .load(DemoMonitor.class.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST).getLoaded();
        for (Method declaredMethod : loaded.getDeclaredMethods()) {
            System.out.println(declaredMethod.getName());
        }
    }

    @Test
    public void test04() throws InvocationTargetException, IllegalAccessException {
        Class<? extends TestMain> loaded = new ByteBuddy().redefine(TestMain.class).visit(Advice.to(SpyAdvice.class).on(ElementMatchers.any())).make()
                .load(DemoMonitor.class.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST).getLoaded();
        for (Method declaredMethod : loaded.getDeclaredMethods()) {
            declaredMethod.invoke(null);
        }

    }

    @Test
    public void test05() throws InvocationTargetException, IllegalAccessException {
        Class<? extends TestMain> loaded = new ByteBuddy().redefine(TestMain.class).visit(Advice.to(SpyAdvice.class).on(ElementMatchers.isAnnotatedWith(Resource.class))).make()
                .load(DemoMonitor.class.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST_PERSISTENT).getLoaded();
        for (Method declaredMethod : loaded.getDeclaredMethods()) {
            System.out.println("========");
            declaredMethod.invoke(null);
            System.out.println("========");
        }
    }
}
