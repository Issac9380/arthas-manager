package com.arthasmanager.arthas.command.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ScCommandTest {

    private ScCommand command;

    @BeforeEach
    void setUp() {
        command = new ScCommand();
    }

    @Test
    void getType_returnsSc() {
        assertThat(command.getType()).isEqualTo("sc");
    }

    @Test
    void getDisplayName_returnsSearchClass() {
        assertThat(command.getDisplayName()).isEqualTo("Search Class (SC)");
    }

    @Test
    void getParams_returnsThreeParams() {
        assertThat(command.getParams()).hasSize(3);
        assertThat(command.getParams()).extracting("name")
                .containsExactly("pattern", "detail", "field");
    }

    @Test
    void buildCommandString_minimal_noFlags() {
        Map<String, Object> params = Map.of("pattern", "com.example.*");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("sc com.example.*");
    }

    @Test
    void buildCommandString_withDetailTrue_appendsD() {
        Map<String, Object> params = Map.of("pattern", "com.example.*Service", "detail", "true");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("sc -d com.example.*Service");
    }

    @Test
    void buildCommandString_withFieldTrue_appendsF() {
        Map<String, Object> params = Map.of("pattern", "com.example.Foo", "field", "true");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("sc -f com.example.Foo");
    }

    @Test
    void buildCommandString_withDetailAndField_appendsBothFlags() {
        Map<String, Object> params = Map.of(
                "pattern", "com.example.Foo",
                "detail", "true",
                "field", "true");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("sc -d -f com.example.Foo");
    }

    @Test
    void buildCommandString_withNoFlags_flagsAreFalse() {
        Map<String, Object> params = Map.of(
                "pattern", "com.example.Bar",
                "detail", "false",
                "field", "false");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("sc com.example.Bar");
    }
}
