package com.luufery.bytebuddy.api.plugin.listener;

import com.luufery.bytebuddy.api.module.RedefinitionHolder;
import net.bytebuddy.agent.builder.AgentBuilder;

import java.util.List;
import java.util.Map;

public class MyRedefinitionListener implements AgentBuilder.RedefinitionStrategy.Listener {

    private String module;

    public MyRedefinitionListener(String module) {
        this.module = module;
    }

    @Override
    public void onBatch(int index, List<Class<?>> batch, List<Class<?>> types) {

    }

    @Override
    public Iterable<? extends List<Class<?>>> onError(int index, List<Class<?>> batch, Throwable throwable, List<Class<?>> types) {
        return null;
    }

    @Override
    public void onComplete(int amount, List<Class<?>> types, Map<List<Class<?>>, Throwable> failures) {
        System.out.println("==========");
        for (Class<?> type : types) {
            System.out.println(type.getName());
        }
        RedefinitionHolder.addAll(module, types);
        System.out.println("==========");
    }
}
