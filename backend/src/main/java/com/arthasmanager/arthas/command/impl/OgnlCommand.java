package com.arthasmanager.arthas.command.impl;

import com.arthasmanager.arthas.command.AbstractArthasCommand;
import com.arthasmanager.arthas.command.CommandParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class OgnlCommand extends AbstractArthasCommand {

    @Override
    public String getType() { return "ognl"; }

    @Override
    public String getDisplayName() { return "OGNL Expression"; }

    @Override
    public String getDescription() {
        return "Execute an arbitrary OGNL expression in the target JVM — read static fields, call methods, etc.";
    }

    @Override
    public List<CommandParam> getParams() {
        return List.of(
            CommandParam.builder()
                .name("expression").label("OGNL Expression").type(CommandParam.ParamType.STRING)
                .required(true)
                .description("OGNL expression, e.g. @System@getProperty(\"java.version\")").build(),
            CommandParam.builder()
                .name("expand").label("Expand Depth").type(CommandParam.ParamType.INTEGER)
                .required(false).defaultValue("1")
                .description("Object expand depth for output").build()
        );
    }

    @Override
    public String buildCommandString(Map<String, Object> params) {
        String expression = str(params, "expression");
        int expand = intVal(params, "expand", 1);
        return "ognl '" + expression + "' -x " + expand;
    }
}
