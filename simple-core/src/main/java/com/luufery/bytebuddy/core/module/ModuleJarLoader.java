package com.luufery.bytebuddy.core.module;

import com.luufery.bytebuddy.api.module.CoreModule;
import com.luufery.bytebuddy.api.spi.definition.PluginDefinitionService;
import com.luufery.bytebuddy.core.classloader.ModuleJarClassLoader;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.UnmodifiableClassException;
import java.util.*;

import static com.luufery.bytebuddy.core.socket.SocketServer.instrumentation;


/**
 * 这里主要管理jar包,也包括{@link ModuleJarClassLoader}的创建与卸载
 */
public class ModuleJarLoader {

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
        try {
            for (PluginDefinitionService service : services) {
                Collection<CoreModule> modules = service.load(moduleJarClassLoader);
                for (CoreModule coreModule : modules) {
                    instrumentation.addTransformer(coreModule.getTransformer(), true);
                    try {
                        instrumentation.retransformClasses(Class.forName(coreModule.getTargetClass()));
                        //TODO 这里我们在reTransform的同时就remove掉了,这个操作可以发生在unload方法中,我们这里为了测试方便直接删除了.
                        instrumentation.removeTransformer(coreModule.getTransformer());
                    } catch (UnmodifiableClassException | ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }finally {
            //TODO 这里同上, 我们在之后也将在unload中卸载.
            moduleJarClassLoader.closeIfPossible();

        }



    }

    public void unload() {

    }

    public void unloadAll(){

    }


}
