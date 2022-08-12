package com.luufery.agent.advice;

import com.luufery.bytebuddy.api.advice.RaspAdvice;
import net.bytebuddy.asm.Advice;

import static net.bytebuddy.implementation.bytecode.assign.Assigner.Typing.DYNAMIC;

public class Demo2Monitor implements RaspAdvice {


    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit() {
        System.out.println("在这里退出了");
    }

}
