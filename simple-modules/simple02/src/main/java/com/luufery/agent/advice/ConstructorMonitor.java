package com.luufery.agent.advice;

import com.luufery.bytebuddy.api.advice.RaspAdvice;
import net.bytebuddy.asm.Advice;

public class ConstructorMonitor implements RaspAdvice {

    @Advice.OnMethodEnter
    public static void enter() {
        System.out.println("这里是构造函数");
    }
}
