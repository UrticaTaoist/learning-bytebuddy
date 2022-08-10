package com.luufery.bytebuddy.api.plugin.util;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class TypeUtils {


    public static ElementMatcher.Junction<TypeDescription> typeMatch(Class<?>[] allLoadedClasses, String[] names) {
        ElementMatcher.Junction<TypeDescription> elementMatcher = ElementMatchers.none();
        //TODO 这里是过程化的,应该抽象
        for (String name : names) {
            for (Class<?> loaded : allLoadedClasses) {
                ElementMatcher.Junction<TypeDescription> subsTypeOf = isSubsTypeOf(loaded, name);
                if (subsTypeOf != null) {
                    elementMatcher = elementMatcher.or(subsTypeOf);
                }
                ElementMatcher.Junction<TypeDescription> supersTypeOf = isSupersTypeOf(loaded, name);
                if (supersTypeOf != null) {
                    elementMatcher = elementMatcher.or(supersTypeOf);
                }
                elementMatcher.or(ElementMatchers.named(name));
            }

        }
        return elementMatcher;
    }


    public static ElementMatcher.Junction<TypeDescription> isSubsTypeOf(Class<?> loadedClass, String clazzName) {
        ElementMatcher.Junction<TypeDescription> subTypeOf = ElementMatchers.none();
        if (loadedClass.getName().equals(clazzName)) {
            subTypeOf = subTypeOf.or(ElementMatchers.isSubTypeOf(loadedClass));
        }
        return subTypeOf;
    }

    public static ElementMatcher.Junction<TypeDescription> isSupersTypeOf(Class<?> loadedClass, String clazzName) {
        ElementMatcher.Junction<TypeDescription> subTypeOf = ElementMatchers.none();
        if (loadedClass.getName().equals(clazzName)) {
            subTypeOf = subTypeOf.or(ElementMatchers.isSuperTypeOf(loadedClass));
        }
        return subTypeOf;
    }


//    public static ElementMatcher.Junction<TypeDescription> isSubsTypeOf(String... classes) {
//        ElementMatcher.Junction<TypeDescription> subTypeOf = ElementMatchers.is(Object.class);
//        for (String clazzName : classes) {
//            Class<?> load = load(clazzName);
//            if (load != null) {
//                subTypeOf = subTypeOf.or(ElementMatchers.isSubTypeOf(load));
//            }
//        }
//        return subTypeOf;
//    }


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
