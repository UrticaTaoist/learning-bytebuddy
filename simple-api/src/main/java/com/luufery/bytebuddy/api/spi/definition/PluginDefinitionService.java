package com.luufery.bytebuddy.api.spi.definition;


import com.luufery.bytebuddy.api.spi.type.AgentTypedSPI;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.util.Collection;

public interface PluginDefinitionService extends AgentTypedSPI {

    Collection<ClassFileTransformer> load(ClassLoader classLoader) throws IOException;
}
