package com.arthasmanager.arthas.command.impl;

import com.arthasmanager.arthas.command.AbstractArthasCommand;
import com.arthasmanager.arthas.command.CommandParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class MonitorCommand extends AbstractArthasCommand {

    @Override
    public String getType() { return "monitor"; }

    @Override
    public String getDisplayName() { return "Monitor"; }

    @Override
    public String getDescription() {
        return "Periodically output invocation statistics for a method (count, RT, fail rate).";
    }

    @Override
    public List<CommandParam> getParams() {
        return List.of(
            CommandParam.builder()
                .name("className").label("Class Name").type(CommandParam.ParamType.STRING)
                .required(true).description("Fully qualified class name").build(),
            CommandParam.builder()
                .name("methodName").label("Method Name").type(CommandParam.ParamType.STRING)
                .required(true).description("Method name to monitor").build(),
            CommandParam.builder()
                .name("cycle").label("Cycle (s)").type(CommandParam.ParamType.INTEGER)
                .required(false).defaultValue("5").description("Statistics aggregation cycle in seconds").build(),
            CommandParam.builder()
                .name("count").label("Max Cycles").type(CommandParam.ParamType.INTEGER)
                .required(false).defaultValue("10").description("Stop after N reporting cycles").build()
        );
    }

    @Override
    public String buildCommandString(Map<String, Object> params) {
        StringBuilder sb = new StringBuilder("monitor");
        sb.append(' ').append(str(params, "className"));
        sb.append(' ').append(str(params, "methodName"));
        sb.append(" -c ").append(intVal(params, "cycle", 5));
        sb.append(" -n ").append(intVal(params, "count", 10));
        return sb.toString();
    }
}
