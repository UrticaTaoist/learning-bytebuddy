package com.luufery.bytebuddy.core.module;

import com.luufery.bytebuddy.api.ModuleException;
import com.luufery.bytebuddy.core.classloader.ModuleJarClassLoader;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultCoreModuleManager implements CoreModuleManager{

    private final Map<String, CoreModule> loadedModuleBOMap = new ConcurrentHashMap<String, CoreModule>();



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
