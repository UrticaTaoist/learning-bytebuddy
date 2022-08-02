package com.luufery.bytebuddy.api.module;

import java.io.File;

/**
 * 用于标注被加载的模块对应的jar包文件和类加载器,便于其他插件装载时的操作.
 */
public interface ModuleJar {
    File getModuleJarFile();

    ClassLoader getClassLoader();
}
