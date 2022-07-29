package com.luufery.bytebuddy.server;

import com.luufery.bytebuddy.server.classloader.ModuleJarClassLoader;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

public class ModuleLoaderTests {

    @Test
    public void load() throws InterruptedException {
        try (ModuleJarClassLoader moduleJarClassLoader = new ModuleJarClassLoader(new File("/Users/luufery/.sandbox-module/jvm-sandbox-demo-1.0-SNAPSHOT-jar-with-dependencies.jar"))) {
            Class<?> aClass = moduleJarClassLoader.loadClass("com.example.ChangeRespModule");
            for (Field declaredField : aClass.getDeclaredFields()) {
                System.out.println(declaredField.getName());
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        Thread.sleep(200000);

    }
}
