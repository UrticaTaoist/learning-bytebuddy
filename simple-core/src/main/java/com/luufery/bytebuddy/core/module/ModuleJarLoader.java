package com.luufery.bytebuddy.core.module;

import com.luufery.bytebuddy.api.module.CoreModule;
import com.luufery.bytebuddy.api.spi.definition.PluginDefinitionService;
import com.luufery.bytebuddy.core.classloader.ModuleJarClassLoader;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.UnmodifiableClassException;
import java.util.*;

import static com.luufery.bytebuddy.core.socket.SocketServer.instrumentation;


/**
 * 这里主要管理jar包,也包括{@link ModuleJarClassLoader}的创建与卸载
 */
@Slf4j
public class ModuleJarLoader {
    private Class<?> cache;

    private static final Map<String, CoreModule> coreModuleMap = new HashMap<String, CoreModule>();

    private static volatile ModuleJarLoader loader;

    public static ModuleJarLoader getInstance() {
        if (loader == null) {
            synchronized (ModuleJarLoader.class) {
                if (loader == null) {
                    return new ModuleJarLoader();
                }
            }
        }
        return loader;
    }

    /**
     * 每一个module都会分配一个独立的{@link ModuleJarClassLoader},也就是说它们是一体的.
     *
     * @param name 通过name在项目目录中查找对应的jar包并加载.
     * @throws IOException .
     */
    public void load(String name) throws IOException {
        //TODO 这里需要把魔法值替换为配置变量.
        File file = new File("/Users/luufery/workspace/com/luufery/learning-bytebuddy/simple-modules/" + name + "/target/" + name + "-1.0-SNAPSHOT-jar-with-dependencies.jar");


        ModuleJarClassLoader moduleJarClassLoader = new ModuleJarClassLoader(file);
        ServiceLoader<PluginDefinitionService> services = ServiceLoader.load(PluginDefinitionService.class, moduleJarClassLoader);


        System.out.println("没有???");
        for (PluginDefinitionService next : services) {
            System.out.println(next.getType());
            CoreModule coreModule = next.load(instrumentation);
//          instrumentation.addTransformer(coreModule.getTransformer(), true);
            try {

//              instrumentation.retransformClasses(coreModule.getTargetClass());
                coreModule.setClassLoader(moduleJarClassLoader);
                coreModuleMap.put(name, coreModule);

            } catch (Exception e) {
                log.warn("module 加载失败,targetClass:{},message:{}", coreModule.getTargetClass(), e.getMessage());
            }
            next.undefineInterceptor();
        }


    }

    public void unload(String name) {
        if (!coreModuleMap.containsKey(name)) {
            return;
        }
        CoreModule coreModule = coreModuleMap.remove(name);
        instrumentation.removeTransformer(coreModule.getTransformer());
        if (coreModule.getClassLoader() instanceof ModuleJarClassLoader) {
            ((ModuleJarClassLoader) coreModule.getClassLoader()).closeIfPossible();
        }

        try {
            instrumentation.retransformClasses(coreModule.getTargetClass());
        } catch (UnmodifiableClassException e) {
            log.warn("reTransform failed", e);
        }
        coreModule.clear();
        System.out.println("啊?");
//        System.gc();
    }

    public void unloadAll() {

    }


}
