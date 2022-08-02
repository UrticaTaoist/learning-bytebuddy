package com.luufery.bytebuddy.api.plugin;

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


    public static <T> Collection<T> newServiceInstances(final Class<T> service,ClassLoader classLoader) {
        List<T> result = new LinkedList<>();
        ServiceLoader.load(service, classLoader).forEach(result::add);
        return result;
    }
}

