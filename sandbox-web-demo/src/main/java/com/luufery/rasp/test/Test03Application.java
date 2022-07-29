package com.luufery.rasp.test;

import com.luufery.bytebuddy.server.util.SpyUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Test03Application {
    public static void main(String[] args) {
        SpyUtils.init("default");

        SpringApplication.run(Test03Application.class, args);

    }


}
