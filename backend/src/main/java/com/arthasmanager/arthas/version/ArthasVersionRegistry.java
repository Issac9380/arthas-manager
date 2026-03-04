package com.arthasmanager.arthas.version;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JDK ↔ Arthas version compatibility registry.
 *
 * <p>Key compatibility constraints:
 * <ul>
 *   <li>JDK 8/11 — all supported Arthas versions (3.4+) work.</li>
 *   <li>JDK 17 — requires Arthas 3.5.4+ (StrongEncapsulation of JDK internals).</li>
 *   <li>JDK 21 — requires Arthas 3.7.0+ (virtual-thread awareness).</li>
 * </ul>
 */
@Component
public class ArthasVersionRegistry {

    /** JDK major version → supported Arthas versions (newest first). */
    private static final Map<String, List<String>> COMPATIBILITY = new LinkedHashMap<>();

    /** JDK major version → recommended Arthas version. */
    private static final Map<String, String> RECOMMENDED = new LinkedHashMap<>();

    static {
        COMPATIBILITY.put("8",  List.of("3.7.2", "3.6.9", "3.5.6", "3.4.8"));
        COMPATIBILITY.put("11", List.of("3.7.2", "3.6.9", "3.5.6", "3.4.8"));
        COMPATIBILITY.put("17", List.of("3.7.2", "3.6.9", "3.5.6"));
        COMPATIBILITY.put("21", List.of("3.7.2"));

        RECOMMENDED.put("8",  "3.7.2");
        RECOMMENDED.put("11", "3.7.2");
        RECOMMENDED.put("17", "3.7.2");
        RECOMMENDED.put("21", "3.7.2");
    }

    /** Returns Arthas versions compatible with the given JDK major version, newest first. */
    public List<String> getCompatibleVersions(String jdkVersion) {
        return COMPATIBILITY.getOrDefault(normalise(jdkVersion), List.of("3.7.2"));
    }

    /** Returns the recommended Arthas version for a given JDK major version. */
    public String getRecommendedVersion(String jdkVersion) {
        return RECOMMENDED.getOrDefault(normalise(jdkVersion), "3.7.2");
    }

    /**
     * Returns the full compatibility matrix for the frontend.
     * Shape: { "8": { recommended: "3.7.2", supported: ["3.7.2", ...] }, ... }
     */
    public Map<String, Object> getVersionMatrix() {
        Map<String, Object> matrix = new LinkedHashMap<>();
        for (String jdk : COMPATIBILITY.keySet()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("recommended", RECOMMENDED.get(jdk));
            entry.put("supported", COMPATIBILITY.get(jdk));
            matrix.put(jdk, entry);
        }
        return matrix;
    }

    /** Returns all known JDK major versions. */
    public List<String> getSupportedJdkVersions() {
        return List.copyOf(COMPATIBILITY.keySet());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /** Strip "jdk-", leading zeros, etc. — "jdk-17" → "17". */
    private String normalise(String jdkVersion) {
        if (jdkVersion == null) return "8";
        return jdkVersion.replaceAll("(?i)^jdk[-_]?", "").replaceAll("\\..*", "").strip();
    }
}
