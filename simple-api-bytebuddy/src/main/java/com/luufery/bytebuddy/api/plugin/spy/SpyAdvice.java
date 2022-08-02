package com.luufery.bytebuddy.api.plugin.spy;

import com.luufery.bytebuddy.api.Spy;
import com.luufery.bytebuddy.api.advice.RaspAdvice;
import net.bytebuddy.asm.Advice;

public class SpyAdvice implements Spy {

    @Advice.OnMethodEnter
    public static void enter() {
        //除了通信,也要完成准入的功能
        System.out.println("TODO 这里可以用于组件间通信");
    }

}
