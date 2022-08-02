package com.luufery.bytebuddy.core.module;




import com.luufery.bytebuddy.api.module.CoreModule;
import com.luufery.bytebuddy.api.module.CoreModuleManager;
import com.luufery.bytebuddy.api.module.ModuleException;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultCoreModuleManager implements CoreModuleManager {

    private final Map<String, CoreModule> loadedModuleBOMap = new ConcurrentHashMap<String, CoreModule>();

//    private final AgentBuilder.Default agentBuilder = new AgentBuilder.Default();


    @Override
    public void active(CoreModule coreModule) {
        //TODO 这里应当做一层代理, 使切点可以响应事件.
        loadedModuleBOMap.put(coreModule.getPoint().getTargetClass(), coreModule);
//        for (RaspTransformationPoint<?> raspTransformationPoint : coreModule.getPoint().getTransformationPoint()) {
//            System.out.println("advice::::" + raspTransformationPoint.getClassOfAdvice());
//            ClassFileTransformer classFileTransformer = agentBuilder.with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
//                    .disableClassFormatChanges()
//                    .type(ElementMatchers.named(coreModule.getTargetClass()))
//                    .transform((DynamicType.Builder<?> builder,
//                                TypeDescription type,
//                                ClassLoader loader,
//                                JavaModule module) -> builder.visit(
//                            Advice.to(raspTransformationPoint.getClassOfAdvice())
//                                    .on(raspTransformationPoint.getMatcher())))
//
//                    .makeRaw();
//            agentBuilder.with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
//                    .disableClassFormatChanges()
//                    .type(ElementMatchers.named(coreModule.getTargetClass()))
//                    .transform((DynamicType.Builder<?> builder,
//                                TypeDescription type,
//                                ClassLoader loader,
//                                JavaModule module) -> builder.visit(
//                            Advice.to(raspTransformationPoint.getClassOfAdvice())
//                                    .on(raspTransformationPoint.getMatcher())))
//
//                    .installOn(instrumentation)
            ;
//        }
    }

    @Override
    public void frozen(CoreModule coreModule) {

    }

    @Override
    public Collection<CoreModule> list() {
        return null;
    }

    @Override
    public CoreModule get(String uniqueId) {
        return null;
    }

    @Override
    public CoreModule unload(CoreModule coreModule, boolean isIgnoreModuleException) throws ModuleException {
        return null;
    }

    @Override
    public void unloadAll() {

    }
}
