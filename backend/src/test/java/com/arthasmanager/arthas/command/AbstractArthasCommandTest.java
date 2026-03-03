package com.arthasmanager.arthas.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for helper methods in {@link AbstractArthasCommand}.
 * Uses a minimal concrete subclass to access protected methods.
 */
class AbstractArthasCommandTest {

    /** Minimal concrete subclass that exposes all protected helpers publicly. */
    static class TestCommand extends AbstractArthasCommand {
        @Override public String getType() { return "test"; }
        @Override public String getDisplayName() { return "Test"; }
        @Override public String getDescription() { return "test"; }
        @Override public List<CommandParam> getParams() { return List.of(); }
        @Override public String buildCommandString(Map<String, Object> params) { return ""; }

        // Expose protected methods
        public String testStr(Map<String, Object> p, String key) { return str(p, key); }
        public String testStr(Map<String, Object> p, String key, String def) { return str(p, key, def); }
        public int testIntVal(Map<String, Object> p, String key, int def) { return intVal(p, key, def); }
        public boolean testBoolVal(Map<String, Object> p, String key) { return boolVal(p, key); }
        public void testAppend(StringBuilder sb, String flag, String value) { append(sb, flag, value); }
        public void testAppendFlag(StringBuilder sb, String flag, boolean cond) { appendFlag(sb, flag, cond); }
    }

    private TestCommand cmd;

    @BeforeEach
    void setUp() {
        cmd = new TestCommand();
    }

    // ── str(params, key) ──────────────────────────────────────────────────────

    @Test
    void str_existingKey_returnsStringValue() {
        assertThat(cmd.testStr(Map.of("k", "hello"), "k")).isEqualTo("hello");
    }

    @Test
    void str_missingKey_returnsEmptyString() {
        assertThat(cmd.testStr(Map.of(), "k")).isEmpty();
    }

    @Test
    void str_nullValue_returnsEmptyString() {
        Map<String, Object> params = new HashMap<>();
        params.put("k", null);
        assertThat(cmd.testStr(params, "k")).isEmpty();
    }

    @Test
    void str_nonStringValue_callsToString() {
        assertThat(cmd.testStr(Map.of("n", 42), "n")).isEqualTo("42");
    }

    // ── str(params, key, default) ─────────────────────────────────────────────

    @Test
    void strWithDefault_presentValue_returnsValue() {
        assertThat(cmd.testStr(Map.of("k", "val"), "k", "default")).isEqualTo("val");
    }

    @Test
    void strWithDefault_missingKey_returnsDefault() {
        assertThat(cmd.testStr(Map.of(), "k", "default")).isEqualTo("default");
    }

    @Test
    void strWithDefault_blankValue_returnsDefault() {
        assertThat(cmd.testStr(Map.of("k", "  "), "k", "default")).isEqualTo("default");
    }

    @Test
    void strWithDefault_emptyString_returnsDefault() {
        assertThat(cmd.testStr(Map.of("k", ""), "k", "default")).isEqualTo("default");
    }

    // ── intVal ────────────────────────────────────────────────────────────────

    @Test
    void intVal_validInteger_parsesCorrectly() {
        assertThat(cmd.testIntVal(Map.of("n", "42"), "n", 0)).isEqualTo(42);
    }

    @Test
    void intVal_integerObject_parsesCorrectly() {
        assertThat(cmd.testIntVal(Map.of("n", 99), "n", 0)).isEqualTo(99);
    }

    @Test
    void intVal_missingKey_returnsDefault() {
        assertThat(cmd.testIntVal(Map.of(), "n", 7)).isEqualTo(7);
    }

    @Test
    void intVal_nullValue_returnsDefault() {
        Map<String, Object> params = new HashMap<>();
        params.put("n", null);
        assertThat(cmd.testIntVal(params, "n", 7)).isEqualTo(7);
    }

    @Test
    void intVal_invalidString_returnsDefault() {
        assertThat(cmd.testIntVal(Map.of("n", "not-a-number"), "n", 5)).isEqualTo(5);
    }

    @Test
    void intVal_negativeInteger_parsesCorrectly() {
        assertThat(cmd.testIntVal(Map.of("n", "-10"), "n", 0)).isEqualTo(-10);
    }

    // ── boolVal ───────────────────────────────────────────────────────────────

    @Test
    void boolVal_trueString_returnsTrue() {
        assertThat(cmd.testBoolVal(Map.of("b", "true"), "b")).isTrue();
    }

    @Test
    void boolVal_falseString_returnsFalse() {
        assertThat(cmd.testBoolVal(Map.of("b", "false"), "b")).isFalse();
    }

    @Test
    void boolVal_missingKey_returnsFalse() {
        assertThat(cmd.testBoolVal(Map.of(), "b")).isFalse();
    }

    @Test
    void boolVal_caseInsensitiveTrue_returnsTrue() {
        assertThat(cmd.testBoolVal(Map.of("b", "TRUE"), "b")).isTrue();
    }

    @Test
    void boolVal_arbitraryString_returnsFalse() {
        assertThat(cmd.testBoolVal(Map.of("b", "yes"), "b")).isFalse();
    }

    // ── append ────────────────────────────────────────────────────────────────

    @Test
    void append_nonBlankValue_appendsFlagAndValue() {
        StringBuilder sb = new StringBuilder("cmd");
        cmd.testAppend(sb, "-x", "hello");
        assertThat(sb.toString()).isEqualTo("cmd -x hello");
    }

    @Test
    void append_blankValue_doesNotAppend() {
        StringBuilder sb = new StringBuilder("cmd");
        cmd.testAppend(sb, "-x", "  ");
        assertThat(sb.toString()).isEqualTo("cmd");
    }

    @Test
    void append_nullValue_doesNotAppend() {
        StringBuilder sb = new StringBuilder("cmd");
        cmd.testAppend(sb, "-x", null);
        assertThat(sb.toString()).isEqualTo("cmd");
    }

    @Test
    void append_emptyValue_doesNotAppend() {
        StringBuilder sb = new StringBuilder("cmd");
        cmd.testAppend(sb, "-x", "");
        assertThat(sb.toString()).isEqualTo("cmd");
    }

    // ── appendFlag ────────────────────────────────────────────────────────────

    @Test
    void appendFlag_conditionTrue_appendsFlag() {
        StringBuilder sb = new StringBuilder("cmd");
        cmd.testAppendFlag(sb, "-v", true);
        assertThat(sb.toString()).isEqualTo("cmd -v");
    }

    @Test
    void appendFlag_conditionFalse_doesNotAppend() {
        StringBuilder sb = new StringBuilder("cmd");
        cmd.testAppendFlag(sb, "-v", false);
        assertThat(sb.toString()).isEqualTo("cmd");
    }
}
