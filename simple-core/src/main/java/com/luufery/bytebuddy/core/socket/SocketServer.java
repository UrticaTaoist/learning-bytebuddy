package com.luufery.bytebuddy.core.socket;

import com.luufery.bytebuddy.core.module.CoreModuleManager;
import com.luufery.bytebuddy.core.module.DefaultCoreModuleManager;
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

public class SocketServer {

    private final Logger logger = LoggerFactory.getLogger(SocketServer.class);

    private ServerSocket server;

    private CoreModuleManager coreModuleManager;

    //先放着
    public static Instrumentation instrumentation;


    public CoreModuleManager getCoreModuleManager() {
        return coreModuleManager;
    }

    private SocketServer() {

    }

    public static SocketServer getInstance() {
        return new SocketServer();
    }

    public Boolean runServer(Map<String, String> config, Instrumentation instrumentation) {
        SocketServer.instrumentation = instrumentation;

        System.out.println("启动socket server!!");

        try (ServerSocket serverSocket = new ServerSocket(18018)) {
            read(serverSocket.accept());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        //这里暂时先这样,还得改.
        coreModuleManager = new DefaultCoreModuleManager();
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

        if (key.startsWith("simple01")) {
            String[] split = key.split(":");
            String pluginName = split[0];
            String methodName = split[1];
            loadJar(pluginName, methodName);
        } else {
            //这里使用spi加载模块
        }


    }

    private static void loadJar(String pluginName, String methodName) {

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
}
