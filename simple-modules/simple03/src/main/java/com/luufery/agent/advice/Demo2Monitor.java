package com.luufery.agent.advice;

import com.luufery.bytebuddy.api.advice.RaspAdvice;
import net.bytebuddy.asm.Advice;

import static net.bytebuddy.implementation.bytecode.assign.Assigner.Typing.DYNAMIC;

public class Demo2Monitor implements RaspAdvice {

    @Advice.OnMethodEnter
    public static long enter() {
        System.out.println("我进来啦!");
        throw new RuntimeException("测试");
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(@Advice.Enter long start,
                            @Advice.Origin String origin,
                            @Advice.Return(readOnly = false, typing = DYNAMIC) String body,
                            @Advice.Thrown(readOnly = false) Throwable throwable
    ) {

    }

}
