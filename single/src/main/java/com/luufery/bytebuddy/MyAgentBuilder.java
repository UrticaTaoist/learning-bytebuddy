package com.luufery.bytebuddy;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;

public class MyAgentBuilder extends AgentBuilder.Default {
    public MyAgentBuilder(ByteBuddy byteBuddy) {
        super(byteBuddy);
    }
}
