package com.luufery.agent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.ResettableClassFileTransformer;

import java.lang.instrument.Instrumentation;
import java.util.Map;
import java.util.Set;

public class MyInstallationListener implements AgentBuilder.InstallationListener {
    @Override
    public void onBeforeInstall(Instrumentation instrumentation, ResettableClassFileTransformer classFileTransformer) {
        System.out.println("马上就要安装啦!::"+classFileTransformer.getClass().getName());
    }

    @Override
    public void onInstall(Instrumentation instrumentation, ResettableClassFileTransformer classFileTransformer) {
        System.out.println("正在安装!::"+classFileTransformer.getClass().getName());

    }

    @Override
    public Throwable onError(Instrumentation instrumentation, ResettableClassFileTransformer classFileTransformer, Throwable throwable) {
        System.out.println("安装出错啦!::"+classFileTransformer.getClass().getName());
        return null;
    }

    @Override
    public void onReset(Instrumentation instrumentation, ResettableClassFileTransformer classFileTransformer) {
        System.out.println("重置了???::"+classFileTransformer.getClass().getName());
    }

    @Override
    public void onBeforeWarmUp(Set<Class<?>> types, ResettableClassFileTransformer classFileTransformer) {
        System.out.println("预热了!");
        for (Class<?> type : types) {
            System.out.println(type.getName());
        }
        System.out.println("预热完成::"+classFileTransformer.getClass().getName());
    }

    @Override
    public void onWarmUpError(Class<?> type, ResettableClassFileTransformer classFileTransformer, Throwable throwable) {
        System.out.println("预热失败!"+type.getName());

    }

    @Override
    public void onAfterWarmUp(Map<Class<?>, byte[]> types, ResettableClassFileTransformer classFileTransformer, boolean transformed) {
        System.out.println("预热之后::");
        for (Class<?> aClass : types.keySet()) {
            System.out.println(aClass.getName());
        }
        System.out.println("预热终了::"+classFileTransformer.getClass().getName());
    }
}
