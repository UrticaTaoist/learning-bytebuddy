package com.luufery.agent;


import java.lang.instrument.Instrumentation;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.luufery.agent.SocketServer.socketServerThread;

public class AgentLauncher {

    public static Instrumentation instrumentation;

    private static final Map<String/*NAMESPACE*/, SandboxClassLoader> sandboxClassLoaderMap
            = new ConcurrentHashMap<String, SandboxClassLoader>();


    public static void agentmain(String args, Instrumentation inst) {
        instrumentation = inst;

        runThread(socketServerThread());
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


    private static void runThread(Thread t) {
//        t.setDaemon(true);
//        t.setContextClassLoader(PluginLoader.getInstance());
        t.start();
    }


}
