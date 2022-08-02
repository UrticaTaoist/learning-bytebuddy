package com.luufery.bytebuddy.api.module;



import java.util.Collection;

public interface CoreModuleManager {

    void active(CoreModule coreModule);

    void frozen(CoreModule coreModule);

    Collection<CoreModule> list();

    CoreModule get(String uniqueId);

    CoreModule unload(CoreModule coreModule, boolean isIgnoreModuleException) throws ModuleException;

    void unloadAll();
}
