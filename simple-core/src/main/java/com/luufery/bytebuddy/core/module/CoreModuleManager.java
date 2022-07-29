package com.luufery.bytebuddy.core.module;


import com.luufery.bytebuddy.api.ModuleException;

import java.util.Collection;

public interface CoreModuleManager {

    void active(CoreModule coreModule);

    void frozen(CoreModule coreModule);

    Collection<CoreModule> list();

    CoreModule get(String uniqueId);

    CoreModule unload(CoreModule coreModule, boolean isIgnoreModuleException) throws ModuleException;

    void unloadAll();
}
