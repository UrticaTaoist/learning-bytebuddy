package com.luufery.bytebuddy.api.plugin.spi;

import javax.annotation.Resource;

public class TestMain {

    @Resource
    public static void test() {
        System.out.println("main");
    }

    public static void test2() {
        System.out.println("main2");
    }


}
