package com.luufery.agent.simple;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.junit.Test;

import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;

public class CacheTests {

    @Test
    public void test00() throws ClassNotFoundException, InterruptedException {
        Class<?> aClass = Class.forName("com.luufery.agent.simple.MyClassloader");

        System.out.println(aClass.getName());
        System.out.println(ManagementFactory.getRuntimeMXBean().getName());
        Thread.sleep(2000);

    }

    @Test
    public void test01() throws ClassNotFoundException, InterruptedException {
        Cache<Class<?>, String> classResult = CacheBuilder.newBuilder().build();
        Class<?> aClass = Class.forName("com.luufery.agent.simple.MyClassloader");
        System.out.println(aClass.getName());
        System.out.println(classResult.getIfPresent(aClass));
        System.out.println(ManagementFactory.getRuntimeMXBean().getName());
        Thread.sleep(2000);
    }

    @Test
    public void test02() {
        Cache<Integer, String> classResult = CacheBuilder.newBuilder().build();
        for (int i = 0; i < 10000; i++) {
            classResult.put(i,"");
        }
        System.out.println(classResult.size());

    }

    @Test
    public void test03(){
        Cache<Integer, String> classResult = CacheBuilder.newBuilder().maximumSize(1000).build();
        for (int i = 0; i < 10000; i++) {
            classResult.put(i,"");
        }
        System.out.println(classResult.size());
    }
}
