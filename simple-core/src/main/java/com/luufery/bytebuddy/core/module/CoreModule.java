package com.luufery.bytebuddy.core.module;

import com.luufery.bytebuddy.api.RaspAdvice;
import lombok.Data;

import java.io.File;
import java.util.Collection;

@Data
public class CoreModule {

    private File moduleJar;

    private ClassLoader classLoader;

    private RaspAdvice advice;
}
