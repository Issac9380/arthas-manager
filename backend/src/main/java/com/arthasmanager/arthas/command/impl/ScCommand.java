package com.arthasmanager.arthas.command.impl;

import com.arthasmanager.arthas.command.AbstractArthasCommand;
import com.arthasmanager.arthas.command.CommandParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ScCommand extends AbstractArthasCommand {

    @Override
    public String getType() { return "sc"; }

    @Override
    public String getDisplayName() { return "Search Class (SC)"; }

    @Override
    public String getDescription() {
        return "Search loaded classes — find which classloader holds a class and its fields.";
    }

    @Override
    public List<CommandParam> getParams() {
        return List.of(
            CommandParam.builder()
                .name("pattern").label("Class Pattern").type(CommandParam.ParamType.STRING)
                .required(true).description("Class name pattern, supports * wildcard, e.g. com.example.*Service").build(),
            CommandParam.builder()
                .name("detail").label("Show Details").type(CommandParam.ParamType.BOOLEAN)
                .required(false).defaultValue("false")
                .description("Show detailed class info including fields (-d)").build(),
            CommandParam.builder()
                .name("field").label("Show Fields").type(CommandParam.ParamType.BOOLEAN)
                .required(false).defaultValue("false")
                .description("Include field information (-f)").build()
        );
    }

    @Override
    public String buildCommandString(Map<String, Object> params) {
        StringBuilder sb = new StringBuilder("sc");
        appendFlag(sb, "-d", boolVal(params, "detail"));
        appendFlag(sb, "-f", boolVal(params, "field"));
        sb.append(' ').append(str(params, "pattern"));
        return sb.toString();
    }
}
