package com.luufery.bytebuddy.api.advice;

/**
 * 用于标注切面
 */
public interface InterceptorPoint {

    /**
     * 标注目标类
     * @return 目标类
     */
    String[] getTargetClass();
}
