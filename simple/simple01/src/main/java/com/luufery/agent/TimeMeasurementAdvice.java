package com.luufery.agent;

import net.bytebuddy.asm.Advice;

import static net.bytebuddy.implementation.bytecode.assign.Assigner.Typing.DYNAMIC;

public class TimeMeasurementAdvice  {
    @Advice.OnMethodEnter
    public static long enter() {
        long l = System.currentTimeMillis();
        System.out.println("就是啊");
        return l;
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(@Advice.Enter long start,
                            @Advice.Origin String origin,
                            @Advice.Return(readOnly = false, typing = DYNAMIC) String body) {
        long l = System.currentTimeMillis();
        long executionTime = l - start;
        System.out.println(origin + " took " + executionTime
                + " to execute");
        body += "反射一个jar???";
    }
}