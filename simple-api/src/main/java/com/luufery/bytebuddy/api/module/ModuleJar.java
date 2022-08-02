package com.luufery.bytebuddy.api.module;

import java.io.File;

public interface ModuleJar {
    File getModuleJarFile();

    ClassLoader getClassLoader();
}
