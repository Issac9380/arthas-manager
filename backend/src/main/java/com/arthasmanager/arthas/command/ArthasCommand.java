package com.arthasmanager.arthas.command;

import java.util.List;
import java.util.Map;

/**
 * Strategy interface — each Arthas command type implements this.
 *
 * <p>Design patterns applied:
 * <ul>
 *   <li><b>Strategy</b>: Encapsulates a family of algorithms (command-building logic)
 *       interchangeable at runtime.</li>
 *   <li><b>Command</b>: Each implementation represents a self-contained Arthas
 *       command with metadata and a build method.</li>
 * </ul>
 */
public interface ArthasCommand {

    /** Unique type identifier used to look up this command in the factory. */
    String getType();

    /** Human-readable name shown in the UI. */
    String getDisplayName();

    /** Short description of what this command does. */
    String getDescription();

    /**
     * Ordered list of parameters that the frontend should render as form fields.
     * The frontend sends values back as {@code Map<String,Object>} params.
     */
    List<CommandParam> getParams();

    /**
     * Build the raw Arthas command string from the user-supplied parameter map.
     * Template Method: concrete commands implement their own construction logic.
     */
    String buildCommandString(Map<String, Object> params);
}
