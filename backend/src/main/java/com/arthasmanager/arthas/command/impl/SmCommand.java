package com.arthasmanager.arthas.command.impl;

import com.arthasmanager.arthas.command.AbstractArthasCommand;
import com.arthasmanager.arthas.command.CommandParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SmCommand extends AbstractArthasCommand {

    @Override
    public String getType() { return "sm"; }

    @Override
    public String getDisplayName() { return "Search Method (SM)"; }

    @Override
    public String getDescription() {
        return "Search the methods of a loaded class, optionally filtered by method name.";
    }

    @Override
    public List<CommandParam> getParams() {
        return List.of(
            CommandParam.builder()
                .name("className").label("Class Pattern").type(CommandParam.ParamType.STRING)
                .required(true).description("Class name pattern, e.g. com.example.UserService").build(),
            CommandParam.builder()
                .name("methodPattern").label("Method Pattern").type(CommandParam.ParamType.STRING)
                .required(false).description("Method name pattern, supports * wildcard").build(),
            CommandParam.builder()
                .name("detail").label("Show Details").type(CommandParam.ParamType.BOOLEAN)
                .required(false).defaultValue("false")
                .description("Show method details including parameter types (-d)").build()
        );
    }

    @Override
    public String buildCommandString(Map<String, Object> params) {
        StringBuilder sb = new StringBuilder("sm");
        appendFlag(sb, "-d", boolVal(params, "detail"));
        sb.append(' ').append(str(params, "className"));
        String method = str(params, "methodPattern");
        if (!method.isBlank()) {
            sb.append(' ').append(method);
        }
        return sb.toString();
    }
}
