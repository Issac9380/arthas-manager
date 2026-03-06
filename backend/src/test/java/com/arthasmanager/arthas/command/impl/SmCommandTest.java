package com.arthasmanager.arthas.command.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SmCommandTest {

    private SmCommand command;

    @BeforeEach
    void setUp() {
        command = new SmCommand();
    }

    @Test
    void getType_returnsSm() {
        assertThat(command.getType()).isEqualTo("sm");
    }

    @Test
    void getDisplayName_returnsSearchMethod() {
        assertThat(command.getDisplayName()).isEqualTo("Search Method (SM)");
    }

    @Test
    void getParams_returnsThreeParams() {
        assertThat(command.getParams()).hasSize(3);
        assertThat(command.getParams()).extracting("name")
                .containsExactly("className", "methodPattern", "detail");
    }

    @Test
    void buildCommandString_minimal_noMethodNoDetail() {
        Map<String, Object> params = Map.of("className", "com.example.Foo");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("sm com.example.Foo");
    }

    @Test
    void buildCommandString_withMethodPattern_appendsMethod() {
        Map<String, Object> params = Map.of(
                "className", "com.example.Foo",
                "methodPattern", "get*");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("sm com.example.Foo get*");
    }

    @Test
    void buildCommandString_withDetailTrue_appendsD() {
        Map<String, Object> params = Map.of(
                "className", "com.example.Foo",
                "detail", "true");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("sm -d com.example.Foo");
    }

    @Test
    void buildCommandString_withDetailAndMethod() {
        Map<String, Object> params = Map.of(
                "className", "com.example.UserService",
                "methodPattern", "find*",
                "detail", "true");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("sm -d com.example.UserService find*");
    }

    @Test
    void buildCommandString_withEmptyMethod_omitsMethod() {
        Map<String, Object> params = Map.of(
                "className", "com.example.Foo",
                "methodPattern", "");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("sm com.example.Foo");
    }
}
