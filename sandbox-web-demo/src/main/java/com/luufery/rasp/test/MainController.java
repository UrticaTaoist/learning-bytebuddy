package com.luufery.rasp.test;

import com.alibaba.jvm.sandbox.spy.Spy;
import com.luufery.rasp.test.module.DemoChangeRespModule;
import com.luufery.bytebuddy.server.manager.MyEventListenerHandler;
import com.luufery.bytebuddy.server.manager.MyModuleEventWatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MainController {

    /**
     * 这里我们直接使用反编译后的sandbox目标类, 可以看到,通过埋点Spy发出事件,根据不同状态对代理的方法做不同的处理.
     * 而sandbox的模块,实际上就是注册一个观察者监听事件.
     * @return .
     * @throws Throwable .
     */
    @GetMapping
    public Object test() throws Throwable {
        boolean var10000 = true;

        boolean var10001;
        int var5;
        try {
            Spy.Ret var10002 = Spy.spyMethodOnBefore(new Object[0], "default", 1001, 1002, "com.boundaryx.rasp.test.MainController", "test", "()Ljava/lang/Object;", this);
            var5 = var10002.state;
            if (var5 != 1) {
                if (var5 != 2) {
                    var10000 = true;
                    System.out.println("sdfsdfsdf");
//                    Thread.sleep(200L);
                    var10001 = true;
                    Spy.Ret var7 = Spy.spyMethodOnReturn("sfsdf", "default", 1001);
                    int var6 = var7.state;
                    if (var6 != 1) {
                        if (var6 != 2) {
                            var10001 = true;
                            return "sfsdf";
                        } else {
                            throw (Throwable) var7.respond;
                        }
                    } else {
                        return var7.respond;
                    }
                } else {
                    throw (Throwable) var10002.respond;
                }
            } else {
                return var10002.respond;
            }
        } catch (Throwable var3) {
            var10001 = true;
            Spy.Ret var4 = Spy.spyMethodOnThrows(var3, "default", 1001);
            var5 = var4.state;
            if (var5 != 1) {
                if (var5 != 2) {
                    var10001 = true;
                    throw var3;
                } else {
                    throw (Throwable) var4.respond;
                }
            } else {
                return var4.respond;
            }
        }
    }


    @GetMapping("frozen")
    public Object frozen() {
        MyEventListenerHandler.getSingleton().frozen(1001);
        return "1001";

    }


    @GetMapping("active")
    public Object active() {
        DemoChangeRespModule changeRespModule = new DemoChangeRespModule();
        changeRespModule.setModuleEventWatcher(new MyModuleEventWatcher());
        changeRespModule.changeResp();
        return "1001";

    }


}
