package com.luufery.bytebuddy.server.socket;

import java.lang.instrument.Instrumentation;
import java.net.ServerSocket;

public class SocketServer {
    public static Thread socketServerThread(Instrumentation instrumentation) {

        return new MyThread(instrumentation);


    }

    public static class MyThread extends Thread {
        private final Instrumentation instrumentation;


        public MyThread(Instrumentation instrumentation) {
            this.instrumentation = instrumentation;

        }

        @Override
        public void run() {
            try {
                System.out.println("启动socket server!!");

                ServerSocket server = new ServerSocket(18018);

                new SocketOperate(server, instrumentation).start();



            } catch (Exception e) {
                e.printStackTrace();
//                System.exit(4);
            } finally {
            }


        }

    }
}
