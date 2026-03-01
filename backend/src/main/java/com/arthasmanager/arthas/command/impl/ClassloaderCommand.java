package com.arthasmanager.arthas.command.impl;

import com.arthasmanager.arthas.command.AbstractArthasCommand;
import com.arthasmanager.arthas.command.CommandParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ClassloaderCommand extends AbstractArthasCommand {

    @Override
    public String getType() { return "classloader"; }

    @Override
    public String getDisplayName() { return "Classloader"; }

    @Override
    public String getDescription() {
        return "List classloaders, inspect their class loading statistics and hierarchies.";
    }

    @Override
    public List<CommandParam> getParams() {
        return List.of(
            CommandParam.builder()
                .name("tree").label("Show Tree").type(CommandParam.ParamType.BOOLEAN)
                .required(false).defaultValue("false")
                .description("Show classloader hierarchy as a tree (-t)").build(),
            CommandParam.builder()
                .name("stats").label("Show Stats").type(CommandParam.ParamType.BOOLEAN)
                .required(false).defaultValue("false")
                .description("Show classloader statistics (-l)").build()
        );
    }

    @Override
    public String buildCommandString(Map<String, Object> params) {
        StringBuilder sb = new StringBuilder("classloader");
        appendFlag(sb, "-t", boolVal(params, "tree"));
        appendFlag(sb, "-l", boolVal(params, "stats"));
        return sb.toString();
    }
}
