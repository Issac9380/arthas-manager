package com.arthasmanager.arthas.command;

import lombok.Builder;
import lombok.Data;

/**
 * Metadata for a single Arthas command parameter.
 * Used by the frontend to render the correct form field dynamically.
 */
@Data
@Builder
public class CommandParam {

    public enum ParamType {
        STRING, INTEGER, BOOLEAN, ENUM
    }

    /** Parameter key, must match the key in ArthasCommandRequest.params */
    private String name;
    private String label;
    private ParamType type;
    private boolean required;
    private String description;
    private String defaultValue;
    /** Valid values for ENUM type */
    private String[] options;
}
