package com.luufery.agent.advice;

import com.luufery.bytebuddy.api.advice.RaspAdvice;
import net.bytebuddy.asm.Advice;

public class Demo31Monitor implements RaspAdvice {

    @Advice.OnMethodEnter
    public static void enter() {
        System.out.println("test辅助切点!!!");
    }


}
