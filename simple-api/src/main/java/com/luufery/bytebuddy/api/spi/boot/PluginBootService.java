package com.luufery.bytebuddy.api.spi.boot;

import com.luufery.bytebuddy.api.config.PluginConfiguration;
import com.luufery.bytebuddy.api.spi.type.AgentTypedSPI;

public interface PluginBootService extends AgentTypedSPI, AutoCloseable {

    void start(PluginConfiguration pluginConfig);

}
