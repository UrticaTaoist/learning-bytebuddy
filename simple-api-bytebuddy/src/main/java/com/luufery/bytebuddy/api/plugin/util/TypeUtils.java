package com.luufery.bytebuddy.api.plugin.util;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

public class TypeUtils {


    public static ElementMatcher.Junction<TypeDescription> isSupersTypeOf(ClassLoader classLoader, String... classes) {
        ElementMatcher.Junction<TypeDescription> subTypeOf = ElementMatchers.is(Object.class);
        for (String clazzName : classes) {
            Class<?> load = load(classLoader, clazzName);
            if (load != null) {
                subTypeOf = subTypeOf.or(ElementMatchers.isSuperTypeOf(load));
            }
        }
        return subTypeOf;
    }

    public static ElementMatcher.Junction<TypeDescription> isSupersTypeOf(String... classes) {
        ElementMatcher.Junction<TypeDescription> subTypeOf = ElementMatchers.is(Object.class);
        for (String clazzName : classes) {
            Class<?> load = load(clazzName);
            if (load != null) {
                subTypeOf = subTypeOf.or(ElementMatchers.isSuperTypeOf(load));
            }
        }
        return subTypeOf;
    }


    public static Class<?> load(String name) {
        try {
            return Class.forName(name);
        } catch (Exception e) {
            return null;
        }
    }

    public static Class<?> load(ClassLoader classLoader, String name) {
        try {
            return classLoader.loadClass(name);
        } catch (Exception e) {
            return null;
        }
    }

}
