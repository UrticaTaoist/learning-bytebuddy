package com.luufery.bytebuddy.core.module;

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
        System.out.println(service.getName());
        System.out.println(service.getClassLoader());
        List<T> result = new LinkedList<>();
        ServiceLoader.load(service, classLoader).forEach((r)->{
            System.out.println("service::::"+r.getClass().getName());
            result.add(r);
        });
        return result;
    }
}

