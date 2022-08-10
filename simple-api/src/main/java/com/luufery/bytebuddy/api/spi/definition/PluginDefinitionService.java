package com.luufery.bytebuddy.api.spi.definition;


import com.luufery.bytebuddy.api.module.ModuleJar;
import com.luufery.bytebuddy.api.Spy;
import com.luufery.bytebuddy.api.advice.RaspAdvice;
import com.luufery.bytebuddy.api.module.CoreModule;
import com.luufery.bytebuddy.api.spi.type.AgentTypedSPI;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.Collection;

/**
 * 这里的接口设计参考了<a href="https://github.com/apache/shardingsphere/tree/master/shardingsphere-agent">shardingsphere-agent</a>
 * 但在最终的实现里,我们没有采用更深入的抽象,即在插件中保留了bytebuddy的依赖, 以便于模块可以更加灵活地操作字节码.
 * 但这回带来安全性问题,所以必须要在core中加入验签的逻辑, 如果插件未经过认证,则不允许加载.
 */
public interface PluginDefinitionService extends AgentTypedSPI {

    /**
     * 通过独立jar包加载模块,在约定里,一个模块目前只允许修改一个类呢,因为我给的key...就
     *
     * @param moduleJar 主要包含类加载器
     * @return 主要返回一个transformer集合和目标Class
     * @throws IOException
     */
    Collection<CoreModule> load(Instrumentation instrumentation) throws IOException;

    /**
     * 这个方法用于织入间谍类,来实现组件间通信和准入
     *
     * @param source 在这里特指模块Advice,这里不限于是bytebuddy还是javassist,具体自己实现去吧.
     * @param spy    间谍类,同样的, 不限定技术栈,但SpyHandle必须一致奥! 但我还没想好该怎么限制..
     * @return 返回一个重新编译的类
     */
    default Class<? extends RaspAdvice> loadSpy(Class<? extends RaspAdvice> source, Class<? extends Spy> spy) {
        throw new RuntimeException("spy notfound");
    }

    void undefineInterceptor(final String classNameOfTarget);

}
