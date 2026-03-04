package com.arthasmanager.arthas.command.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MonitorCommandTest {

    private MonitorCommand command;

    @BeforeEach
    void setUp() {
        command = new MonitorCommand();
    }

    @Test
    void getType_returnsMonitor() {
        assertThat(command.getType()).isEqualTo("monitor");
    }

    @Test
    void getDisplayName_returnsMonitor() {
        assertThat(command.getDisplayName()).isEqualTo("Monitor");
    }

    @Test
    void getParams_returnsFourParams() {
        assertThat(command.getParams()).hasSize(4);
        assertThat(command.getParams()).extracting("name")
                .containsExactly("className", "methodName", "cycle", "count");
    }

    @Test
    void buildCommandString_minimal_usesDefaults() {
        Map<String, Object> params = Map.of(
                "className", "com.example.Foo",
                "methodName", "bar");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("monitor com.example.Foo bar -c 5 -n 10");
    }

    @Test
    void buildCommandString_withCustomCycle() {
        Map<String, Object> params = Map.of(
                "className", "com.example.Foo",
                "methodName", "bar",
                "cycle", "60");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("monitor com.example.Foo bar -c 60 -n 10");
    }

    @Test
    void buildCommandString_withCustomCount() {
        Map<String, Object> params = Map.of(
                "className", "com.example.Foo",
                "methodName", "bar",
                "count", "3");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("monitor com.example.Foo bar -c 5 -n 3");
    }

    @Test
    void buildCommandString_withCustomCycleAndCount() {
        Map<String, Object> params = Map.of(
                "className", "com.example.UserService",
                "methodName", "login",
                "cycle", "30",
                "count", "5");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("monitor com.example.UserService login -c 30 -n 5");
    }

    @Test
    void buildCommandString_withInvalidCycle_usesDefault() {
        Map<String, Object> params = Map.of(
                "className", "com.example.Foo",
                "methodName", "bar",
                "cycle", "notANumber");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("monitor com.example.Foo bar -c 5 -n 10");
    }
}
