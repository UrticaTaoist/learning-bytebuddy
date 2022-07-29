package com.luufery.bytebuddy.core.util;

import org.apache.commons.lang3.reflect.MethodUtils;

import java.lang.reflect.InvocationTargetException;

public class AgentUtils {

    private void uninstall() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final Class<?> classOfAgentLauncher = getClass().getClassLoader()
                .loadClass("com.luufery.agent.AgentLauncher");

        MethodUtils.invokeStaticMethod(
                classOfAgentLauncher,
                "uninstall",
                "default"/* 这里要获取配置啊 */
        );
    }
}
