package com.luufery.bytebuddy.core.module;

import com.google.common.collect.ImmutableMap;
import com.luufery.bytebuddy.api.point.PluginInterceptorPoint;
import com.luufery.bytebuddy.api.spi.definition.AbstractPluginDefinitionService;
import com.luufery.bytebuddy.api.spi.definition.PluginDefinitionService;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarFile;

public class SpiPluginLauncher {

//    private static volatile Map<String/*NAME*/, PluginJar> spiPluginMap
//            = new ConcurrentHashMap<>();

    public static final List<PluginJar> jars = new ArrayList<>();


    public static Map<String, PluginInterceptorPoint> interceptorPointMap = new HashMap<>();


    public static void loadAllPlugins(ClassLoader classLoader) throws IOException {
        Map<String, PluginInterceptorPoint> pointMap = new HashMap<>();

        loadPluginDefinitionServices(new HashSet<>(), pointMap, classLoader);


        System.out.println("?????????");
        System.out.println(pointMap.size());
        System.out.println("?????????");

        interceptorPointMap = ImmutableMap.<String, PluginInterceptorPoint>builder().putAll(pointMap).build();
    }

    private static void loadPluginDefinitionServices(final Set<String> ignoredPluginNames, final Map<String, PluginInterceptorPoint> pointMap, ClassLoader classLoader) {
        System.out.println("classloader::::" + classLoader.getClass().getName());
        PluginServiceLoader.newServiceInstances(PluginDefinitionService.class, classLoader)
                .stream()
                .filter(each -> ignoredPluginNames.isEmpty() || !ignoredPluginNames.contains(each.getType()))
                .forEach(each -> buildPluginInterceptorPointMap(each, pointMap));
    }

    private static void buildPluginInterceptorPointMap(final PluginDefinitionService pluginDefinitionService, final Map<String, PluginInterceptorPoint> pointMap) {
        AbstractPluginDefinitionService definitionService = (AbstractPluginDefinitionService) pluginDefinitionService;
        definitionService.install().forEach(each -> {
            String target = each.getTargetClass();
            //如果已存在key,确保所有的方法advice都被添加
            if (pointMap.containsKey(target)) {
                PluginInterceptorPoint pluginInterceptorPoint = pointMap.get(target);
                pluginInterceptorPoint.getTransformationPoint().addAll(each.getTransformationPoint());
            } else {
                pointMap.put(target, each);
            }
        });
    }

    public void loadPlugin(String name) {

    }


    @RequiredArgsConstructor
    public static class PluginJar {

        public final JarFile jarFile;

        public final File sourcePath;
    }

}
