package com.luufery.bytebuddy.core.socket;

import com.luufery.bytebuddy.api.module.CoreModuleManager;
import com.luufery.bytebuddy.core.module.DefaultCoreModuleManager;
import com.luufery.bytebuddy.core.module.ModuleJarLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

/**
 * 这里暂时使用简易的{@link ServerSocket}完成插件控制,之后可以考虑适配其他的.
 */
public class SocketServer {

    private final Logger logger = LoggerFactory.getLogger(SocketServer.class);

    private ServerSocket server;

    private CoreModuleManager coreModuleManager;

    //先放着
    public static Instrumentation instrumentation;


    public static CoreModuleManager getCoreModuleManager() {
        if (getInstance().coreModuleManager == null) {
            synchronized (CoreModuleManager.class) {
                if (getInstance().coreModuleManager == null) {
                    return new DefaultCoreModuleManager();
                }
            }
        }
        return getInstance().coreModuleManager;
    }

    private static volatile SocketServer socketServer;

    private SocketServer() {

    }

    public static SocketServer getInstance() {
        if (socketServer == null) {
            synchronized (SocketServer.class) {
                if (socketServer == null) {
                    return new SocketServer();
                }
            }
        }
        return socketServer;
    }

    public Boolean runServer(Map<String, String> config, Instrumentation instrumentation) {
        SocketServer.instrumentation = instrumentation;

        System.out.println("启动socket server!!");

        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    server = new ServerSocket(18018);
                    read(server.accept());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //这里暂时先这样,还得改.
                coreModuleManager = new DefaultCoreModuleManager();
            }
        };
        thread.setDaemon(true);
        thread.start();

        return true;

    }


    private static void read(Socket socket) {
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
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void doActions(String key) {

        //这里使用spi加载模块
        try {
            String[] split = key.split(":");
            String action = split[0];
            String plugin = split[1];
            if (action.equals("load")) {
                ModuleJarLoader.getInstance().load(plugin);

            } else if (action.equals("unload")) {
                ModuleJarLoader.getInstance().unload(plugin);
            }

        } catch (IOException e) {
            System.out.println("============");
            System.out.println(e.getMessage());
            System.out.println("============");
        }
    }

    public void destroy() {
        if (server != null)
            try {
                server.close();
            } catch (IOException e) {
                logger.warn(e.getMessage());
                throw new RuntimeException(e);
            }
    }

    public static class MyThread extends Thread {

    }
}
