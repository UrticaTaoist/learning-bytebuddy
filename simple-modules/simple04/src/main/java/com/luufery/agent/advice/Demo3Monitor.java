package com.luufery.agent.advice;

import com.luufery.bytebuddy.api.advice.RaspAdvice;
import net.bytebuddy.asm.Advice;

import static net.bytebuddy.implementation.bytecode.assign.Assigner.Typing.DYNAMIC;

public class Demo3Monitor implements RaspAdvice {

    @Advice.OnMethodEnter
    public static void enter() {
        System.out.println("javaagent运行啦");
    }


}
