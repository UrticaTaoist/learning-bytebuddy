package com.luufery.bytebuddy.api.spi.definition;


import com.luufery.bytebuddy.api.point.PluginInterceptorPoint;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class AbstractPluginDefinitionService implements PluginDefinitionService {

    private final Map<String, PluginInterceptorPoint.Builder> interceptorPointMap = new HashMap<>();

    /**
     * 自定义拦截器处理拦截点
     */
    public abstract void defineInterceptors();

    /**
     * 初始化拦截点集合
     *
     * @return 插件拦截点集合
     */
    public final Collection<PluginInterceptorPoint> install() {
        defineInterceptors();
        return interceptorPointMap.values().stream().map(PluginInterceptorPoint.Builder::install).collect(Collectors.toList());
    }

    /**
     * 自定义拦截器,不在上面使用,而是在每个插件中使用到
     * @param classNameOfTarget class
     * @return 拦截点构建器
     */
    protected final PluginInterceptorPoint.Builder defineInterceptor(final String classNameOfTarget) {
        if (interceptorPointMap.containsKey(classNameOfTarget)) {
            return interceptorPointMap.get(classNameOfTarget);
        }
        PluginInterceptorPoint.Builder builder = PluginInterceptorPoint.intercept(classNameOfTarget);
        interceptorPointMap.put(classNameOfTarget, builder);
        return builder;
    }
}
