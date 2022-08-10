package com.luufery.rasp.test;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class Test {
    public static void main(String[] args) {
//        ThreadFactory build = new ThreadFactoryBuilder().setNameFormat("CLOUDRASP-GrpcManager").build();


//        Thread thread = new Thread(() -> {
////            try {
//                System.out.println("???");
////                Thread.sleep(2);
////            } catch (InterruptedException e) {
////                System.out.println("终止了啊");
////                throw new Error(e);
////            }
//        });
////        try {
//            ScheduledFuture<?> scheduledFuture = new ScheduledThreadPoolExecutor(1).
//                    scheduleWithFixedDelay(thread, 0, 1, TimeUnit.SECONDS);
////            thread.interrupt();
//            System.out.println("interrupt");
//            scheduledFuture.cancel(true);
////        }catch (RuntimeException e){
////            System.out.println(e.getMessage()+".");
////        }

    }
}
