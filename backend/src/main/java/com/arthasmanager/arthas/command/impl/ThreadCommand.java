package com.arthasmanager.arthas.command.impl;

import com.arthasmanager.arthas.command.AbstractArthasCommand;
import com.arthasmanager.arthas.command.CommandParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ThreadCommand extends AbstractArthasCommand {

    @Override
    public String getType() { return "thread"; }

    @Override
    public String getDisplayName() { return "Thread"; }

    @Override
    public String getDescription() {
        return "List all threads or print the stack trace of a specific thread.";
    }

    @Override
    public List<CommandParam> getParams() {
        return List.of(
            CommandParam.builder()
                .name("id").label("Thread ID").type(CommandParam.ParamType.INTEGER)
                .required(false).description("Print stack trace for this thread ID; leave blank to list all").build(),
            CommandParam.builder()
                .name("top").label("Top N (CPU)").type(CommandParam.ParamType.INTEGER)
                .required(false).defaultValue("5")
                .description("Show top N threads by CPU usage").build(),
            CommandParam.builder()
                .name("deadlock").label("Detect Deadlock").type(CommandParam.ParamType.BOOLEAN)
                .required(false).defaultValue("false")
                .description("Detect and report deadlocked threads").build()
        );
    }

    @Override
    public String buildCommandString(Map<String, Object> params) {
        String id       = str(params, "id");
        String top      = str(params, "top");
        boolean deadlock = boolVal(params, "deadlock");

        if (!id.isBlank()) {
            return "thread " + id;
        }
        if (deadlock) {
            return "thread -b";
        }
        StringBuilder sb = new StringBuilder("thread");
        if (!top.isBlank()) {
            sb.append(" -n ").append(top);
        }
        return sb.toString();
    }
}
