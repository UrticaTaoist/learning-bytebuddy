package com.luufery.bytebuddy.api.module;

import com.luufery.bytebuddy.api.InterceptorPoint;
import lombok.Data;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;

@Data
public class CoreModule {

    private String targetClass;

    private File moduleJar;

    private ClassLoader classLoader;

    private ClassFileTransformer transformer;

}
