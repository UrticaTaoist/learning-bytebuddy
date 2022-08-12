package com.luufery.bytebuddy.api.plugin.spi;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PluginServiceLoader {

    /**
     * 加载SPI列表
     *
     * @param service spi
     * @param <T>     自定义泛型,PluginDefinitionService
     * @return 列表
     */


    public static <T> Collection<T> newServiceInstances(final Class<T> service, ClassLoader classLoader) {
        List<T> result = new LinkedList<T>();
        for (T t : ServiceLoader.load(service, classLoader)) {
            result.add(t);
        }
        return result;
    }
}

