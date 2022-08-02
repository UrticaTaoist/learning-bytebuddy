package com.luufery.bytebuddy.api.module;



import java.util.Collection;

/**
 * 我们会在这里管理模块,管理其状态
 */
public interface CoreModuleManager {

    /**
     * 通过名字加载模块,这个名字一般会映射到文件夹并加载
     * @param name 模块名
     */
    void load(String name);

    void active(CoreModule coreModule);

    void frozen(CoreModule coreModule);

    Collection<CoreModule> list();

    CoreModule get(String uniqueId);

    CoreModule unload(CoreModule coreModule, boolean isIgnoreModuleException) throws ModuleException;

    void unloadAll();
}
