package com.luufery.bytebuddy;

import net.bytebuddy.asm.Advice;

import static net.bytebuddy.implementation.bytecode.assign.Assigner.Typing.DYNAMIC;

public class DemoMonitor  {

    @Advice.OnMethodEnter
    public static void enter() {
        System.out.println("我进来啦!");
    }


}
