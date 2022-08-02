package com.luufery.bytebuddy.api.module;

import com.luufery.bytebuddy.api.InterceptorPoint;
import lombok.Data;

import java.io.File;

@Data
public class CoreModule {

    private String targetClass;

    private File moduleJar;

    private ClassLoader classLoader;

    private InterceptorPoint point;
}
