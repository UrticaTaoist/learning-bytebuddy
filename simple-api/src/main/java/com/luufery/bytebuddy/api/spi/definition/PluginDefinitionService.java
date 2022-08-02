package com.luufery.bytebuddy.api.spi.definition;


import com.luufery.bytebuddy.api.ModuleJar;
import com.luufery.bytebuddy.api.module.CoreModule;
import com.luufery.bytebuddy.api.spi.type.AgentTypedSPI;

import java.io.IOException;
import java.util.Collection;

public interface PluginDefinitionService extends AgentTypedSPI {

    Collection<CoreModule> load(ModuleJar moduleJar) throws IOException;
}
