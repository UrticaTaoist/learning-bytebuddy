package com.luufery.bytebuddy.server.util;


import com.alibaba.jvm.sandbox.spy.Spy;
import com.luufery.bytebuddy.server.manager.MyEventListenerHandler;

/**
 * Spy类操作工具类
 *
 * @author luajia@taobao.com
 */
public class SpyUtils {


    /**
     * 初始化Spy类
     *
     * @param namespace 命名空间
     */
    public synchronized static void init(final String namespace) {

        if (!Spy.isInit(namespace)) {
            Spy.init(namespace, MyEventListenerHandler.getSingleton());
        }

    }

    /**
     * 清理Spy中的命名空间
     *
     * @param namespace 命名空间
     */
    public synchronized static void clean(final String namespace) {
        Spy.clean(namespace);
    }

}
