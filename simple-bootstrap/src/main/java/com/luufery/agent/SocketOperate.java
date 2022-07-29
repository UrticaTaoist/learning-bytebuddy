package com.luufery.agent;


import java.io.*;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

import static com.luufery.agent.AgentLauncher.loadOrDefineClassLoader;


public class SocketOperate extends Thread implements Closeable {
    private static Socket socket;
    private static ServerSocket currentServer;


    public SocketOperate(ServerSocket server) {
        currentServer = server;
        try {
            socket = server.accept();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    @Override
    public void run() {
        try {
            //封装输入流（接收客户端的流）
            BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());
            DataInputStream dis = new DataInputStream(bis);
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            byte[] bytes = new byte[1];

            StringBuilder ret = new StringBuilder();

            while (dis.read(bytes) != -1) {

                ret.append(MessageParsing.bytesToHexString(bytes));

                if (dis.available() == 0) {

                    String string = MessageParsing.hexStringToString(ret.toString());
                    System.out.println(string);

                    dos.flush();
                    ret = new StringBuilder();

                    if (string.equals("shutdown")) {
                        break;
                    }
                    doActions(string);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("client is over");
            if (socket != null) {
                try {
                    socket.close();
                    socket = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (currentServer != null) {
                try {
                    currentServer.close();
                    currentServer = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void uninstall() throws IOException {
        socket.close();
        currentServer.close();
        socket = null;
        currentServer = null;
    }

    private void doActions(String key) {


        String[] split = key.split(":");
        String pluginName = split[0];
        String methodName = split[1];
        loadJar(pluginName, methodName);

    }

    private void loadJar(String pluginName, String methodName) {

        try {
            ClassLoader classLoader = loadOrDefineClassLoader("default", "/Users/luufery/workspace/com/luufery/learning-bytebuddy/simple/" + pluginName + "/target/" + pluginName + "-1.0-SNAPSHOT-jar-with-dependencies.jar");
            Class<?> loadClass = classLoader.loadClass("com.luufery.agent.MyPlugin");
            Method method = loadClass.getDeclaredMethod(methodName, Instrumentation.class);
            method.invoke(null, AgentLauncher.instrumentation);
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            //这里借用JVM-Sandbox的实现,卸载ClassLoader,避免无法更新插件.
            //但这里的一个现象还未能解释,如果不close,重新加载新的jar时,之前的代码增强也失效了,transform确实执行了呀.
            //这里的ClassLoader我们用完即删了, 其实可以保留的,sandbox的用法是SandboxClassLoader用于加载核心包,这里面的类一般会一直使用.
            //然后再由SandboxClassLoader关联加载ModuleClassLoader,这样的话,卸载插件时只会关闭ModuleClassLoader,而不会影响核心包.
            //注意,在Sandbox中,核心包中的jetty服务器也是由SandboxClassLoader负责加载的,我们的例子中只使用了默认类加载器.
            AgentLauncher.uninstall("default");
            //这里实测啊..close之后并不需要主动gc, 之前一直有用这个来着.
//            System.gc();
        }
    }

    @Override
    public void close() throws IOException {
        uninstall();
    }
}




