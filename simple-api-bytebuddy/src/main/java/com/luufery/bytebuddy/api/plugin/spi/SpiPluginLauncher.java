package com.luufery.bytebuddy.api.plugin.spi;

import com.luufery.bytebuddy.api.plugin.point.PluginInterceptorPoint;
import com.luufery.bytebuddy.api.spi.definition.PluginDefinitionService;

import java.io.IOException;
import java.util.*;

/**
 * 这是以前使用的SPI加载器,稍加改动,删除了类加载部分,只保留ServiceLoader.
 */
public class SpiPluginLauncher {

//    public static Map<String, PluginInterceptorPoint> interceptorPointMap = new HashMap<>();


    public static Map<String, PluginInterceptorPoint> loadAllPlugins(ClassLoader classLoader) throws IOException {
        Map<String, PluginInterceptorPoint> pointMap = new HashMap<>();

        loadPluginDefinitionServices(new HashSet<>(), pointMap, classLoader);

        return pointMap;
//        interceptorPointMap = ImmutableMap.<String, PluginInterceptorPoint>builder().putAll(pointMap).build();
    }

    private static void loadPluginDefinitionServices(final Set<String> ignoredPluginNames, final Map<String, PluginInterceptorPoint> pointMap, ClassLoader classLoader) {
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


}
