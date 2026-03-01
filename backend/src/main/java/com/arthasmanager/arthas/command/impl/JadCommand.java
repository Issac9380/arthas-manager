package com.arthasmanager.arthas.command.impl;

import com.arthasmanager.arthas.command.AbstractArthasCommand;
import com.arthasmanager.arthas.command.CommandParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class JadCommand extends AbstractArthasCommand {

    @Override
    public String getType() { return "jad"; }

    @Override
    public String getDisplayName() { return "Decompile (JAD)"; }

    @Override
    public String getDescription() {
        return "Decompile a loaded class back to Java source code.";
    }

    @Override
    public List<CommandParam> getParams() {
        return List.of(
            CommandParam.builder()
                .name("className").label("Class Name").type(CommandParam.ParamType.STRING)
                .required(true).description("Fully qualified class name to decompile").build(),
            CommandParam.builder()
                .name("methodName").label("Method Name").type(CommandParam.ParamType.STRING)
                .required(false).description("Only decompile this specific method (optional)").build(),
            CommandParam.builder()
                .name("source").label("Show Source Only").type(CommandParam.ParamType.BOOLEAN)
                .required(false).defaultValue("true")
                .description("Suppress class info header, show only source").build()
        );
    }

    @Override
    public String buildCommandString(Map<String, Object> params) {
        StringBuilder sb = new StringBuilder("jad");
        appendFlag(sb, "--source-only", boolVal(params, "source"));
        sb.append(' ').append(str(params, "className"));
        String method = str(params, "methodName");
        if (!method.isBlank()) {
            sb.append(' ').append(method);
        }
        return sb.toString();
    }
}
