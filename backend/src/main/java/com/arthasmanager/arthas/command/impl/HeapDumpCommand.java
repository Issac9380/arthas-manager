package com.arthasmanager.arthas.command.impl;

import com.arthasmanager.arthas.command.AbstractArthasCommand;
import com.arthasmanager.arthas.command.CommandParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class HeapDumpCommand extends AbstractArthasCommand {

    @Override
    public String getType() { return "heapdump"; }

    @Override
    public String getDisplayName() { return "Heap Dump"; }

    @Override
    public String getDescription() {
        return "Dump the JVM heap to a .hprof file inside the container for offline analysis.";
    }

    @Override
    public List<CommandParam> getParams() {
        return List.of(
            CommandParam.builder()
                .name("filePath").label("Output File Path").type(CommandParam.ParamType.STRING)
                .required(false).defaultValue("/tmp/arthas-heapdump.hprof")
                .description("Absolute path inside the container where the dump will be written").build(),
            CommandParam.builder()
                .name("liveOnly").label("Live Objects Only").type(CommandParam.ParamType.BOOLEAN)
                .required(false).defaultValue("true")
                .description("Dump only live objects (reduces file size)").build()
        );
    }

    @Override
    public String buildCommandString(Map<String, Object> params) {
        String filePath = str(params, "filePath", "/tmp/arthas-heapdump.hprof");
        boolean liveOnly = boolVal(params, "liveOnly");
        StringBuilder sb = new StringBuilder("heapdump");
        appendFlag(sb, "--live", liveOnly);
        sb.append(' ').append(filePath);
        return sb.toString();
    }
}
