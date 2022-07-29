package com.luufery.agent;


import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class AgentLauncher {

    private static final Map<String/*NAMESPACE*/, SandboxClassLoader> sandboxClassLoaderMap
            = new ConcurrentHashMap<String, SandboxClassLoader>();


    public static void agentmain(String args, Instrumentation inst) {

        install(inst, toFeatureMap(args));
//        runThread(socketServerThread());
    }

    private static final String KEY_NAMESPACE = "namespace";

    private static final String KEY_CORE = "core";

    private static final String DEFAULT_NAMESPACE = "default";
    private static final String DEFAULT_CORE = "simple-core";


    /**
     * 这里我们参考Sandbox的实现方式,把Server放在core里面,这样以保证资源隔离,在agent层只有一个ClassLoader,其他的啥也没有.
     *
     * @param instrumentation instrumentation
     */
    private static void install(Instrumentation instrumentation, Map<String, String> config) {
        try {
            final ClassLoader sandboxClassLoader = loadOrDefineClassLoader(
                    getNamespace(config),
                    getCoreJar(config)
                    // SANDBOX_CORE_JAR_PATH
            );
            Class<?> socketServerClass = sandboxClassLoader.loadClass("com.luufery.bytebuddy.core.socket.SocketServer");
            Object server = socketServerClass.getDeclaredMethod("getInstance").invoke(null);
            try {
                Method runServer = socketServerClass.getDeclaredMethod("runServer", Map.class, Instrumentation.class);
                runServer.invoke(server, config, instrumentation);
            } catch (Throwable e) {
                Method destroy = socketServerClass.getDeclaredMethod("destroy");
                destroy.invoke(server);
                throw new RuntimeException(e);
            }
        } catch (Throwable cause) {
            uninstall(getNamespace(config));
            throw new RuntimeException("sandbox attach failed.", cause);
        }

    }

    private static String getNamespace(final Map<String, String> config) {
        return getDefault(config, KEY_NAMESPACE, DEFAULT_NAMESPACE);
    }


    private static String getCoreJar(final Map<String, String> config) {
        String coreJarPath = getDefault(config, KEY_CORE, DEFAULT_CORE);
        if (isWindows()) {
            Matcher m = Pattern.compile("(?i)^[/\\\\]([a-z])[/\\\\]").matcher(coreJarPath);
            if (m.find()) {
                coreJarPath = m.replaceFirst("$1:/");
            }
        }
        return coreJarPath;
    }


    private static String OS = System.getProperty("os.name").toLowerCase();

    private static boolean isWindows() {
        return OS.contains("win");
    }

    private static String getDefault(final Map<String, String> map, final String key, final String defaultValue) {
        return null != map
                && !map.isEmpty()
                ? getDefaultString(map.get(key), defaultValue)
                : defaultValue;
    }

    private static Map<String, String> toFeatureMap(final String featureString) {
        final Map<String, String> featureMap = new LinkedHashMap<String, String>();

        // 不对空字符串进行解析
        if (isBlankString(featureString)) {
            return featureMap;
        }

        // KV对片段数组
        final String[] kvPairSegmentArray = featureString.split(";");
        if (kvPairSegmentArray.length <= 0) {
            return featureMap;
        }

        for (String kvPairSegmentString : kvPairSegmentArray) {
            if (isBlankString(kvPairSegmentString)) {
                continue;
            }
            final String[] kvSegmentArray = kvPairSegmentString.split("=");
            if (kvSegmentArray.length != 2
                    || isBlankString(kvSegmentArray[0])
                    || isBlankString(kvSegmentArray[1])) {
                continue;
            }
            featureMap.put(kvSegmentArray[0], kvSegmentArray[1]);
        }

        return featureMap;
    }

    private static boolean isNotBlankString(final String string) {
        return null != string
                && string.length() > 0
                && !string.matches("^\\s*$");
    }

    private static boolean isBlankString(final String string) {
        return !isNotBlankString(string);
    }

    private static String getDefaultString(final String string, final String defaultString) {
        return isNotBlankString(string)
                ? string
                : defaultString;
    }

    public static synchronized ClassLoader loadOrDefineClassLoader(final String namespace,
                                                                   final String coreJar) throws Throwable {

        final SandboxClassLoader classLoader;

        // 如果已经被启动则返回之前启动的ClassLoader
        if (sandboxClassLoaderMap.containsKey(namespace)
                && null != sandboxClassLoaderMap.get(namespace)) {
            classLoader = sandboxClassLoaderMap.get(namespace);
        }

        // 如果未启动则重新加载
        else {
            classLoader = new SandboxClassLoader(namespace, coreJar);
            sandboxClassLoaderMap.put(namespace, classLoader);
        }

        return classLoader;
    }

    public static synchronized ClassLoader loadOrDefineClassLoader(final String namespace,
                                                                   final String[] coreJars) throws Throwable {

        final SandboxClassLoader classLoader;

        // 如果已经被启动则返回之前启动的ClassLoader
        if (sandboxClassLoaderMap.containsKey(namespace)
                && null != sandboxClassLoaderMap.get(namespace)) {
            classLoader = sandboxClassLoaderMap.get(namespace);
        }

        // 如果未启动则重新加载
        else {
            classLoader = new SandboxClassLoader(namespace, Arrays.stream(coreJars).map(path -> {
                try {
                    return new URL("file:" + path);
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }).toArray(URL[]::new));
            sandboxClassLoaderMap.put(namespace, classLoader);
        }

        return classLoader;
    }

    public static synchronized void uninstall(final String namespace) {
        final SandboxClassLoader sandboxClassLoader = sandboxClassLoaderMap.get(namespace);
        if (null == sandboxClassLoader) {
            return;
        }

        // 关闭服务器
//        final Class<?> classOfProxyServer = sandboxClassLoader.loadClass(CLASS_OF_PROXY_CORE_SERVER);
//        classOfProxyServer.getMethod("destroy")
//                .invoke(classOfProxyServer.getMethod("getInstance").invoke(null));

        // 关闭SandboxClassLoader
        sandboxClassLoader.closeIfPossible();
        sandboxClassLoaderMap.remove(namespace);
    }


}
