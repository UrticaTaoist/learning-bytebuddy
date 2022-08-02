package com.luufery.bytebuddy.api.spi.type;

/**
 * 标注插件类型,如..Mysql,Risk等等, 这里本来是用作唯一ID使用的, 但现在看有待商榷.
 */
public interface AgentTypedSPI {

    String getType();

}
