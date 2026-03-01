package com.arthasmanager.arthas.factory;

import com.arthasmanager.arthas.command.ArthasCommand;
import com.arthasmanager.arthas.command.CommandParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Factory that discovers all {@link ArthasCommand} beans and provides
 * lookup + metadata APIs used by controllers.
 *
 * <p>Design pattern: <b>Factory</b> — centralises command creation/lookup,
 * decoupling callers from concrete implementations.
 * Spring auto-injects every registered {@code ArthasCommand} bean,
 * so new commands become available simply by adding a {@code @Component}.
 */
@Component
public class ArthasCommandFactory {

    private final Map<String, ArthasCommand> registry;

    public ArthasCommandFactory(List<ArthasCommand> commands) {
        registry = commands.stream()
                .collect(Collectors.toMap(ArthasCommand::getType, c -> c));
    }

    public ArthasCommand getCommand(String type) {
        ArthasCommand command = registry.get(type);
        if (command == null) {
            throw new IllegalArgumentException("Unknown Arthas command type: " + type);
        }
        return command;
    }

    /** Returns metadata for every registered command (used by frontend to render menus). */
    public List<Map<String, Object>> listCommandMeta() {
        return registry.values().stream()
                .map(c -> Map.<String, Object>of(
                        "type", c.getType(),
                        "displayName", c.getDisplayName(),
                        "description", c.getDescription(),
                        "params", c.getParams().stream()
                                .map(this::paramToMap)
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }

    private Map<String, Object> paramToMap(CommandParam p) {
        return Map.of(
                "name", p.getName(),
                "label", p.getLabel(),
                "type", p.getType().name(),
                "required", p.isRequired(),
                "description", p.getDescription() != null ? p.getDescription() : "",
                "defaultValue", p.getDefaultValue() != null ? p.getDefaultValue() : ""
        );
    }
}
