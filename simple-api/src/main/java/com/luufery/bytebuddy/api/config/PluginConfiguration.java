package com.luufery.bytebuddy.api.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public  class PluginConfiguration {

    private String host;

    private int port;

    private String password;

    private Map<String,Object> configmap;
}

