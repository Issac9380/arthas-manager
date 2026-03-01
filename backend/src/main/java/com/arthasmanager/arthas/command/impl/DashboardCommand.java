package com.arthasmanager.arthas.command.impl;

import com.arthasmanager.arthas.command.AbstractArthasCommand;
import com.arthasmanager.arthas.command.CommandParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DashboardCommand extends AbstractArthasCommand {

    @Override
    public String getType() { return "dashboard"; }

    @Override
    public String getDisplayName() { return "Dashboard"; }

    @Override
    public String getDescription() {
        return "Real-time dashboard showing CPU, memory, threads, GC, and runtime info.";
    }

    @Override
    public List<CommandParam> getParams() {
        return List.of(
            CommandParam.builder()
                .name("interval").label("Refresh Interval (ms)").type(CommandParam.ParamType.INTEGER)
                .required(false).defaultValue("5000")
                .description("Refresh interval in milliseconds").build(),
            CommandParam.builder()
                .name("count").label("Number of Iterations").type(CommandParam.ParamType.INTEGER)
                .required(false).defaultValue("1")
                .description("How many times to print the dashboard (use 1 for a snapshot)").build()
        );
    }

    @Override
    public String buildCommandString(Map<String, Object> params) {
        StringBuilder sb = new StringBuilder("dashboard");
        int interval = intVal(params, "interval", 5000);
        int count    = intVal(params, "count", 1);
        sb.append(" -i ").append(interval);
        sb.append(" -n ").append(count);
        return sb.toString();
    }
}
