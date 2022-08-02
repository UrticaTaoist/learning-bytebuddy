package com.luufery.bytebuddy.api.module;

import com.luufery.bytebuddy.api.InterceptorPoint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;

@Data
@Builder
public class CoreModule {

    private String targetClass;

    private File moduleJar;

    private ClassLoader classLoader;

    private ClassFileTransformer transformer;

}
