package com.luufery.rasp.test.module;

import com.alibaba.jvm.sandbox.api.ProcessController;
import com.alibaba.jvm.sandbox.api.annotation.Command;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;

import javax.annotation.Resource;

public class DemoChangeRespModule {
    private final static String CLASS_NAME = "com.luuuuuuu.rasp.test.MainController";
    private final static String METHOD_NAME = "test";
    private ModuleEventWatcher moduleEventWatcher;

    public void setModuleEventWatcher(ModuleEventWatcher moduleEventWatcher) {
        this.moduleEventWatcher = moduleEventWatcher;
    }

    public void changeResp(){
        new EventWatchBuilder(moduleEventWatcher).onClass(CLASS_NAME).onBehavior(METHOD_NAME).onWatch(new AdviceListener(){

            protected void afterReturning(Advice advice) throws Throwable {
                System.out.println("afterReturning");
                ProcessController.returnImmediately(advice.getReturnObj() +": now changed");
            }

        });
    }
}
