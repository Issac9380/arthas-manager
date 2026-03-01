package com.arthasmanager.arthas.command.impl;

import com.arthasmanager.arthas.command.AbstractArthasCommand;
import com.arthasmanager.arthas.command.CommandParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class StackCommand extends AbstractArthasCommand {

    @Override
    public String getType() { return "stack"; }

    @Override
    public String getDisplayName() { return "Stack"; }

    @Override
    public String getDescription() {
        return "Print the full call stack leading to a specific method invocation.";
    }

    @Override
    public List<CommandParam> getParams() {
        return List.of(
            CommandParam.builder()
                .name("className").label("Class Name").type(CommandParam.ParamType.STRING)
                .required(true).description("Fully qualified class name").build(),
            CommandParam.builder()
                .name("methodName").label("Method Name").type(CommandParam.ParamType.STRING)
                .required(true).description("Method name").build(),
            CommandParam.builder()
                .name("condition").label("Condition Filter").type(CommandParam.ParamType.STRING)
                .required(false).description("OGNL condition to filter calls").build(),
            CommandParam.builder()
                .name("count").label("Max Matches").type(CommandParam.ParamType.INTEGER)
                .required(false).defaultValue("5").description("Stop after N matches").build()
        );
    }

    @Override
    public String buildCommandString(Map<String, Object> params) {
        StringBuilder sb = new StringBuilder("stack");
        sb.append(' ').append(str(params, "className"));
        sb.append(' ').append(str(params, "methodName"));
        String condition = str(params, "condition");
        if (!condition.isBlank()) {
            sb.append(" '").append(condition).append("'");
        }
        sb.append(" -n ").append(intVal(params, "count", 5));
        return sb.toString();
    }
}
