package com.arthasmanager.arthas.command.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JadCommandTest {

    private JadCommand command;

    @BeforeEach
    void setUp() {
        command = new JadCommand();
    }

    @Test
    void getType_returnsJad() {
        assertThat(command.getType()).isEqualTo("jad");
    }

    @Test
    void getDisplayName_returnsDecompileJad() {
        assertThat(command.getDisplayName()).isEqualTo("Decompile (JAD)");
    }

    @Test
    void getParams_returnsThreeParams() {
        assertThat(command.getParams()).hasSize(3);
        assertThat(command.getParams()).extracting("name")
                .containsExactly("className", "methodName", "source");
    }

    @Test
    void buildCommandString_withSourceOnly_includesFlag() {
        Map<String, Object> params = Map.of(
                "className", "com.example.Foo",
                "source", "true");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("jad --source-only com.example.Foo");
    }

    @Test
    void buildCommandString_withMethodName_appendsMethod() {
        Map<String, Object> params = Map.of(
                "className", "com.example.Foo",
                "methodName", "bar",
                "source", "true");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("jad --source-only com.example.Foo bar");
    }

    @Test
    void buildCommandString_withSourceFalse_omitsFlag() {
        Map<String, Object> params = Map.of(
                "className", "com.example.Foo",
                "source", "false");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("jad com.example.Foo");
        assertThat(cmd).doesNotContain("--source-only");
    }

    @Test
    void buildCommandString_withNoSourceParam_omitsFlag() {
        // boolVal returns false when key absent → no --source-only flag
        Map<String, Object> params = new HashMap<>();
        params.put("className", "com.example.Foo");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("jad com.example.Foo");
        assertThat(cmd).doesNotContain("--source-only");
    }

    @Test
    void buildCommandString_withEmptyMethodName_omitsMethod() {
        Map<String, Object> params = Map.of(
                "className", "com.example.Foo",
                "methodName", "",
                "source", "false");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("jad com.example.Foo");
    }
}
