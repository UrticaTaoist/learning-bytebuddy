package com.luufery.agent.simple;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TypeTests {

    @Test
    public void test() {
        TypeDescription of = TypeDescription.ForLoadedType.of(null);

    }


    @Test
    public void test2() throws ClassNotFoundException {

//        Class<?> load = load("java.nio.file.Filessss");
//        if(load)
        ElementMatcher.Junction<TypeDescription> typeOf = isSupersTypeOf("java.nio.file.Files", "java.nio.file.Filessss");
        System.out.println(typeOf.toString());

    }

    private ElementMatcher.Junction<TypeDescription> isSupersTypeOf(String... classes) {
        ElementMatcher.Junction<TypeDescription> subTypeOf = ElementMatchers.is(Object.class);
        for (String clazzName : classes) {
            Class<?> load = load(clazzName);
            if (load != null) {
                subTypeOf = subTypeOf.or(ElementMatchers.isSuperTypeOf(load));
            }
        }
        return subTypeOf;
    }


    private Class<?> load(String name) {
        try {
            return Class.forName(name);
        } catch (Exception e) {
            return null;
        }
    }


}
