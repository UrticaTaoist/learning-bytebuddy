package com.luufery.bytebuddy.core.module;


import com.luufery.bytebuddy.api.module.CoreModule;
import com.luufery.bytebuddy.api.module.CoreModuleManager;
import com.luufery.bytebuddy.api.module.ModuleException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 这是默认的CoreModuleManager实现. 我们会在这里统一处理控制中心的指令
 */
@Slf4j
public class DefaultCoreModuleManager implements CoreModuleManager {

    private final Map<String, CoreModule> loadedModuleBOMap = new ConcurrentHashMap<String, CoreModule>();

    @Override
    public void load(String name) {
        try {
            ModuleJarLoader.getInstance().load(name);
        } catch (IOException e) {
            log.warn("模块加载失败", e);
        }
    }

    @Override
    public void active(CoreModule coreModule) {
    }

    @Override
    public void frozen(CoreModule coreModule) {

    }

    @Override
    public Collection<CoreModule> list() {
        return null;
    }

    @Override
    public CoreModule get(String uniqueId) {
        return null;
    }

    @Override
    public CoreModule unload(CoreModule coreModule, boolean isIgnoreModuleException) throws ModuleException {

        return null;
    }

    @Override
    public void unloadAll() {

    }
}
