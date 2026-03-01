package com.arthasmanager.arthas.command.impl;

import com.arthasmanager.arthas.command.AbstractArthasCommand;
import com.arthasmanager.arthas.command.CommandParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class WatchCommand extends AbstractArthasCommand {

    @Override
    public String getType() { return "watch"; }

    @Override
    public String getDisplayName() { return "Watch"; }

    @Override
    public String getDescription() {
        return "Observe method parameters, return values, and exceptions at runtime.";
    }

    @Override
    public List<CommandParam> getParams() {
        return List.of(
            CommandParam.builder()
                .name("className").label("Class Name").type(CommandParam.ParamType.STRING)
                .required(true).description("Fully qualified class name, e.g. com.example.UserService").build(),
            CommandParam.builder()
                .name("methodName").label("Method Name").type(CommandParam.ParamType.STRING)
                .required(true).description("Method name to watch").build(),
            CommandParam.builder()
                .name("express").label("OGNL Expression").type(CommandParam.ParamType.STRING)
                .required(false).defaultValue("{params, returnObj, throwExp}")
                .description("OGNL expression to observe").build(),
            CommandParam.builder()
                .name("condition").label("Condition Filter").type(CommandParam.ParamType.STRING)
                .required(false).description("OGNL condition to filter invocations, e.g. params[0]>0").build(),
            CommandParam.builder()
                .name("count").label("Max Matches").type(CommandParam.ParamType.INTEGER)
                .required(false).defaultValue("5").description("Stop after N matches").build(),
            CommandParam.builder()
                .name("expand").label("Expand Depth").type(CommandParam.ParamType.INTEGER)
                .required(false).defaultValue("1").description("Object expand depth for output").build(),
            CommandParam.builder()
                .name("beforeInvoke").label("Before Invocation").type(CommandParam.ParamType.BOOLEAN)
                .required(false).defaultValue("false").description("Observe before method entry (-b)").build(),
            CommandParam.builder()
                .name("onException").label("On Exception").type(CommandParam.ParamType.BOOLEAN)
                .required(false).defaultValue("true").description("Observe on exception (-e)").build()
        );
    }

    @Override
    public String buildCommandString(Map<String, Object> params) {
        String className  = str(params, "className");
        String methodName = str(params, "methodName");
        String express    = str(params, "express", "{params, returnObj, throwExp}");
        String condition  = str(params, "condition");
        int    count      = intVal(params, "count", 5);
        int    expand     = intVal(params, "expand", 1);
        boolean before    = boolVal(params, "beforeInvoke");
        boolean onException = boolVal(params, "onException");

        StringBuilder sb = new StringBuilder("watch");
        sb.append(' ').append(className);
        sb.append(' ').append(methodName);
        sb.append(" \"").append(express).append('"');

        if (!condition.isBlank()) {
            sb.append(" '").append(condition).append("'");
        }
        appendFlag(sb, "-b", before);
        appendFlag(sb, "-e", onException);
        sb.append(" -n ").append(count);
        sb.append(" -x ").append(expand);
        return sb.toString();
    }
}
