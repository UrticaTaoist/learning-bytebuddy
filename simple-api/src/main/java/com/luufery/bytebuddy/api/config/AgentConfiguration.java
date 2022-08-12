package com.luufery.bytebuddy.api.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 这里沿袭了shardingsphere的配置模式,待后续的需求变更再改.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgentConfiguration {

    private String applicationName;

    private Set<String> ignoredPluginNames = new HashSet<String>();

    private Map<String, PluginConfiguration> plugins = new HashMap<String,PluginConfiguration>();



}

