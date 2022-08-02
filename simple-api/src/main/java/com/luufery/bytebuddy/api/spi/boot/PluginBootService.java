package com.luufery.bytebuddy.api.spi.boot;

import com.luufery.bytebuddy.api.config.PluginConfiguration;
import com.luufery.bytebuddy.api.spi.type.AgentTypedSPI;

/**
 * 这里原本的用途是为插件提供一个守护线程的启动规范,但到目前为止也没有应用
 */
public interface PluginBootService extends AgentTypedSPI, AutoCloseable {

    void start(PluginConfiguration pluginConfig);

    default void stop() throws Exception {
        this.close();
    }


}
