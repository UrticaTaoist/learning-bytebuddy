package com.luufery.bytebuddy.api.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 这里沿袭了shardingsphere的配置模式,待后续的需求变更再改.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public  class PluginConfiguration {

    private String host;

    private int port;

    private String password;

    private Map<String,Object> configmap;
}

