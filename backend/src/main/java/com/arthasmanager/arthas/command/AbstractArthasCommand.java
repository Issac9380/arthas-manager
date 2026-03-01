package com.arthasmanager.arthas.command;

import java.util.Map;
import java.util.Optional;

/**
 * Template Method base class for all Arthas commands.
 *
 * <p>Provides utility helpers so concrete commands stay concise:
 * {@link #str}, {@link #intVal}, {@link #boolVal}, {@link #append}.
 */
public abstract class AbstractArthasCommand implements ArthasCommand {

    // ── Helpers ──────────────────────────────────────────────────────────────

    protected String str(Map<String, Object> params, String key) {
        return Optional.ofNullable(params.get(key))
                .map(Object::toString)
                .orElse("");
    }

    protected String str(Map<String, Object> params, String key, String defaultValue) {
        String v = str(params, key);
        return v.isBlank() ? defaultValue : v;
    }

    protected int intVal(Map<String, Object> params, String key, int defaultValue) {
        try {
            return Optional.ofNullable(params.get(key))
                    .map(v -> Integer.parseInt(v.toString()))
                    .orElse(defaultValue);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    protected boolean boolVal(Map<String, Object> params, String key) {
        return Boolean.parseBoolean(str(params, key));
    }

    /**
     * Appends {@code flag value} to builder if value is non-blank.
     */
    protected void append(StringBuilder sb, String flag, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(' ').append(flag).append(' ').append(value);
        }
    }

    /**
     * Appends {@code flag} only when condition is true.
     */
    protected void appendFlag(StringBuilder sb, String flag, boolean condition) {
        if (condition) {
            sb.append(' ').append(flag);
        }
    }
}
