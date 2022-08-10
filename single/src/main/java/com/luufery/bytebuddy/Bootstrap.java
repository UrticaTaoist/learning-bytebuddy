package com.luufery.bytebuddy;


import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.List;

public class Bootstrap {

    public static Instrumentation instrumentation;

    public static void agentmain(String args, Instrumentation inst) {
        instrumentation = inst;
        AgentLauncher.install();
    }

    public static void premain(String args, Instrumentation inst) {
        instrumentation = inst;
        AgentLauncher.install();

    }

//    public static void main(String[] args) throws AgentLoadException, IOException, AgentInitializationException, AttachNotSupportedException, InterruptedException {
//        System.out.println("running JVM start ");
//        List<VirtualMachineDescriptor> list = VirtualMachine.list();
//        for (VirtualMachineDescriptor vmd : list) {
//            System.out.println(vmd.displayName());
////            if (vmd.displayName().contains("Bootstrap")) {
//            if (vmd.displayName().contains("Test03Application")) {
//                System.out.println(vmd.id());
//                VirtualMachine virtualMachine = VirtualMachine.attach(vmd.id());
//                virtualMachine.loadAgent(
//                        "/Users/luufery/workspace/com/luufery/learning-bytebuddy/single/target/single-1.0-SNAPSHOT-jar-with-dependencies.jar"
//                );
//                virtualMachine.detach();
//                System.out.println("attach");
//
//                //home=/Users/luufery/workspace/com/github/alibaba/jvm-sandbox/target/sandbox/bin/..;token=37558994029;server.ip=0.0.0.0;server.port=0;namespace=default
//            }
//        }
//        Thread.sleep(1000L);
//    }
}
