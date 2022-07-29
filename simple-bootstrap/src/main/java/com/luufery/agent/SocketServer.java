package com.luufery.agent;

import java.lang.instrument.Instrumentation;
import java.net.ServerSocket;

public class SocketServer {
    public static Thread socketServerThread() {

        return new Thread(() -> {
            try {
                System.out.println("启动socket server!!");

                ServerSocket server = new ServerSocket(18018);

                new SocketOperate(server).start();


            } catch (Exception e) {
                e.printStackTrace();
//                System.exit(4);
            }

        });


    }
}
