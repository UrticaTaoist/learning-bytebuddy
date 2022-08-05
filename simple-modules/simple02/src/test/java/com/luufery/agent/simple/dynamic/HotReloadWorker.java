package com.luufery.agent.simple.dynamic;

import java.io.File;
import java.io.FileInputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.charset.StandardCharsets.UTF_8;

public class HotReloadWorker {

    private static final Map<String, Class<?>> CLASS_CACHE = new ConcurrentHashMap<>();


    public static void doReload(Instrumentation instrumentation, String replaceTargetFile) {
        try {
            if (replaceTargetFile == null) {
                return;
            }
            File file = Paths.get(replaceTargetFile).toFile();
            if (replaceTargetFile.endsWith(FileExtension.CLASS_FILE_EXTENSION)) {
                byte[] newClazzByteCode = Files.readAllBytes(file.toPath());
                String className = ClassByteCodeUtils.getClassNameFromByteCode(newClazzByteCode);
                doReloadClassFile(instrumentation, className, newClazzByteCode);
            } else if (replaceTargetFile.endsWith(FileExtension.JAVA_FILE_EXTENSION)) {
                byte[] newClazzSourceBytes = Files.readAllBytes(file.toPath());
                String className = ClassByteCodeUtils.getClassNameFromSourceCode(IOUtils.toString(new FileInputStream(file)));
                doCompileThenReloadClassFile(instrumentation, className, new String(newClazzSourceBytes, UTF_8));
            }
        } catch (Exception e) {
//            log.error("class reload failed: {}", replaceTargetFile, e);
            e.printStackTrace();
        }
    }

    private static void doCompileThenReloadClassFile(Instrumentation instrumentation, String className,
                                                     String sourceCode) {
        ClassLoader classLoader = getClassLoader(className, instrumentation);
        System.out.printf("Target class %s class loader %s \n", className, classLoader);
        DynamicCompiler dynamicCompiler = new DynamicCompiler(classLoader);
        dynamicCompiler.addSource(className, sourceCode);
        Map<String, byte[]> classNameToByteCodeMap = dynamicCompiler.buildByteCodes();
        classNameToByteCodeMap.forEach((clazzName, bytes) -> {
            try {
                doReloadClassFile(instrumentation, clazzName, bytes);
            } catch (Exception e) {
                System.err.println("Class " + clazzName + " reload error ");
            }
        });
    }

    private static ClassLoader getClassLoader(String className, Instrumentation instrumentation) {
        Class<?> targetClass = findTargetClass(className, instrumentation);
        if (targetClass != null) {
            return targetClass.getClassLoader();
        }
        return HotReloadWorker.class.getClassLoader();
    }

    public static void doReloadClassFile(Instrumentation instrumentation, String className,
                                         byte[] newClazzByteCode) throws UnmodifiableClassException, ClassNotFoundException {
        Class<?> clazz = getToReloadClass(instrumentation, className, newClazzByteCode);
        if (clazz == null) {
//            log.error("Class " + className + " not found");
            System.err.println("Class " + className + " not found");
        } else {
            instrumentation.redefineClasses(new ClassDefinition(clazz, newClazzByteCode));
            System.out.println("Class: " + clazz + " reload success!");
        }
    }

    private static Class<?> getToReloadClass(Instrumentation instrumentation, String className,
                                             byte[] newClazzByteCode) {
        Class<?> clazz = findTargetClass(className, instrumentation);
        if (clazz == null) {
            clazz = defineNewClass(className, newClazzByteCode, clazz);
        }
        return clazz;
    }

    private static Class<?> defineNewClass(String className, byte[] newClazzByteCode, Class<?> clazz) {
        System.out.println("Class " + className + " not found, try to define a new class");
        ClassLoader classLoader = HotReloadWorker.class.getClassLoader();
        try {
            Method defineClass = ClassLoader.class.getDeclaredMethod("defineClass", String.class,
                    byte[].class, int.class, int.class);
            defineClass.setAccessible(true);
            clazz = (Class<?>) defineClass.invoke(classLoader, className, newClazzByteCode
                    , 0, newClazzByteCode.length);
            System.out.println("Class " + className + " define success " + clazz);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            System.err.printf("defineNewClass %s failed ", className);
        }
        return clazz;
    }

    protected static Class<?> findTargetClass(String className, Instrumentation instrumentation) {
        return CLASS_CACHE.computeIfAbsent(className, clazzName -> {
            Class<?>[] allLoadedClasses = instrumentation.getAllLoadedClasses();

            Arrays.stream(allLoadedClasses).parallel()
                    .filter(clazz -> clazzName.equals(clazz.getName()))
                    .forEach(aClass -> System.out.println("已发现:" + className + "@" + aClass.hashCode()));

            return Arrays.stream(allLoadedClasses)
                    .parallel()
                    .filter(clazz -> clazzName.equals(clazz.getName()))
                    .findFirst()
                    .orElse(null);
        });
    }
}
