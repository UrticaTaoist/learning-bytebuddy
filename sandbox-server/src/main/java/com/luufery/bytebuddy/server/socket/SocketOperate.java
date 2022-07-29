package com.luufery.bytebuddy.server.socket;


import java.io.*;
import java.lang.instrument.Instrumentation;
import java.net.ServerSocket;
import java.net.Socket;


public class SocketOperate extends Thread implements Closeable {
    private static Socket socket;
    private static ServerSocket currentServer;

    private static Instrumentation currentInstrumentation;

    public SocketOperate(ServerSocket server, Instrumentation instrumentation) {
        currentServer = server;
        try {
            socket = server.accept();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        currentInstrumentation = instrumentation;
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
                    dos.flush();
                    ret = new StringBuilder();
                    doActions(string);
                    if (string.equals("shutdown")) {
                        break;
                    }
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
        currentInstrumentation = null;
    }

    private void doActions(String key) {

        switch (key) {
            case "fff": {
                break;
            }

            default: {
                System.out.println(key);
            }
        }
    }

    @Override
    public void close() throws IOException {
        uninstall();
    }
}




