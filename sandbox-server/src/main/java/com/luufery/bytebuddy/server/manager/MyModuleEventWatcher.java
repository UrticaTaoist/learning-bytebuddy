package com.luufery.bytebuddy.server.manager;

import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.filter.Filter;
import com.alibaba.jvm.sandbox.api.listener.EventListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchCondition;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import com.alibaba.jvm.sandbox.api.util.Sequencer;
import com.alibaba.jvm.sandbox.api.util.matcher.ExtFilterMatcher;
import com.alibaba.jvm.sandbox.api.util.matcher.Matcher;

import static com.alibaba.jvm.sandbox.api.filter.ExtFilter.ExtFilterFactory.make;
import static com.alibaba.jvm.sandbox.api.util.matcher.ExtFilterMatcher.toOrGroupMatcher;

public class MyModuleEventWatcher implements ModuleEventWatcher {

    private final Sequencer watchIdSequencer = new Sequencer();

    @Override
    public int watch(Filter filter, EventListener listener, Progress progress, Event.Type... eventType) {
        return watch(new ExtFilterMatcher(make(filter)), listener, progress, eventType);
    }

    @Override
    public int watch(Filter filter, EventListener listener, Event.Type... eventType) {
        return watch(filter, listener, null, eventType);
    }

    @Override
    public int watch(EventWatchCondition condition, EventListener listener, Progress progress, Event.Type... eventType) {
        return watch(toOrGroupMatcher(condition.getOrFilterArray()), listener, progress, eventType);
    }

    private int watch(final Matcher matcher,
                      final EventListener listener,
                      final Progress progress,
                      final Event.Type... eventType) {
        final int watchId = watchIdSequencer.next();
        MyEventListenerHandler.getSingleton()
                .active(1001, listener, eventType);
        return watchId;
    }

    @Override
    public void delete(int watcherId, Progress progress) {

        //sandbox在这里主要是根据watchId清除对应的SandboxClassFileTransformer
        // 并冻结所有关联代码增强,我们在测试中直接通过接口冻结.
//        EventListenerHandler.getSingleton()
//                .frozen(sandboxClassFileTransformer.getListenerId());
        // 同时在JVM中移除掉命中的ClassFileTransformer
//        inst.removeTransformer(sandboxClassFileTransformer);

        //把所有Matcher对应的类重新渲染
    }

    @Override
    public void delete(int watcherId) {
        delete(watcherId,null);
    }

    @Override
    public void watching(Filter filter, EventListener listener, Progress wProgress, WatchCallback watchCb, Progress dProgress, Event.Type... eventType) throws Throwable {

    }

    @Override
    public void watching(Filter filter, EventListener listener, WatchCallback watchCb, Event.Type... eventType) throws Throwable {

    }
}
