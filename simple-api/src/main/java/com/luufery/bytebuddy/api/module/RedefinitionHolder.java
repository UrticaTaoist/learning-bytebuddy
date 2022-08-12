package com.luufery.bytebuddy.api.module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RedefinitionHolder {

    //TODO 没必要用ThreadLocal吧?
    private static final ThreadLocal<Map<String, List<Class<?>>>> map = new ThreadLocal<Map<String, List<Class<?>>>>();


    public synchronized static List<Class<?>> getLoadedClass(String module) {
        if (map.get() != null)
            return map.get().get(module);
        else
            return new ArrayList<Class<?>>();
    }

    public synchronized static void addAll(String key, List<Class<?>> classes) {
        if (map.get() == null) {
            map.set(new HashMap<String, List<Class<?>>>());
        }
        map.get().put(key, classes);
    }

    public synchronized static void clear(String module) {
        if (map.get() != null)
            map.get().remove(module);
    }

    public synchronized static void remove() {
        map.remove();
    }
}
