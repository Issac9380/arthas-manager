package com.arthasmanager.arthas.command.impl;

import com.arthasmanager.arthas.command.AbstractArthasCommand;
import com.arthasmanager.arthas.command.CommandParam;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class JvmCommand extends AbstractArthasCommand {

    @Override
    public String getType() { return "jvm"; }

    @Override
    public String getDisplayName() { return "JVM Info"; }

    @Override
    public String getDescription() {
        return "Display JVM system properties, memory, GC, and class-loading information.";
    }

    @Override
    public List<CommandParam> getParams() {
        return Collections.emptyList();
    }

    @Override
    public String buildCommandString(Map<String, Object> params) {
        return "jvm";
    }
}
