package com.luufery.bytebuddy.api.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgentConfiguration {

    private String applicationName;

    private Set<String> ignoredPluginNames = new HashSet<>();

    private Map<String, PluginConfiguration> plugins = new HashMap<>();



}

