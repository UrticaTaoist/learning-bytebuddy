package com.luufery.bytebuddy.api;

import java.io.File;

public interface ModuleJar {
    File getModuleJarFile();

    ClassLoader getClassLoader();
}
