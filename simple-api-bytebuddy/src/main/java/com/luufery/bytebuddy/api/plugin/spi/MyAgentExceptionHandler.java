package com.luufery.bytebuddy.api.plugin.spi;

import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
public class MyAgentExceptionHandler implements Advice.ExceptionHandler {

//    Logger logger = LoggerFactory.getLogger(MyAgentExceptionHandler.class);
    @Override
    public StackManipulation resolve(MethodDescription instrumentedMethod, TypeDescription instrumentedType) {
        try {
//            return MethodInvocation.invoke(new MethodDescription.ForLoadedMethod(Throwable.class.getMethod("printStackTrace")));
            return MethodInvocation.invoke(new MethodDescription.ForLoadedMethod(Throwable.class.getMethod("printStackTrace")));
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("Cannot locate Throwable::printStackTrace");
        }
    }
    public static void print(){
        System.out.println("这是在干啥");
    }
}
