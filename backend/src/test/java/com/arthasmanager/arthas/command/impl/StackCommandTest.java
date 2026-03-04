package com.arthasmanager.arthas.command.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StackCommandTest {

    private StackCommand command;

    @BeforeEach
    void setUp() {
        command = new StackCommand();
    }

    @Test
    void getType_returnsStack() {
        assertThat(command.getType()).isEqualTo("stack");
    }

    @Test
    void getDisplayName_returnsStack() {
        assertThat(command.getDisplayName()).isEqualTo("Stack");
    }

    @Test
    void getParams_returnsFourParams() {
        assertThat(command.getParams()).hasSize(4);
        assertThat(command.getParams()).extracting("name")
                .containsExactly("className", "methodName", "condition", "count");
    }

    @Test
    void buildCommandString_minimal_usesDefaultCount() {
        Map<String, Object> params = Map.of(
                "className", "com.example.Foo",
                "methodName", "bar");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("stack com.example.Foo bar -n 5");
    }

    @Test
    void buildCommandString_withCondition_appendsCondition() {
        Map<String, Object> params = Map.of(
                "className", "com.example.Foo",
                "methodName", "bar",
                "condition", "#p0>0");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("stack com.example.Foo bar '#p0>0' -n 5");
    }

    @Test
    void buildCommandString_withCustomCount() {
        Map<String, Object> params = Map.of(
                "className", "com.example.Foo",
                "methodName", "bar",
                "count", "2");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("stack com.example.Foo bar -n 2");
    }

    @Test
    void buildCommandString_withConditionAndCount() {
        Map<String, Object> params = Map.of(
                "className", "com.example.OrderService",
                "methodName", "save",
                "condition", "#cost>50",
                "count", "10");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("stack com.example.OrderService save '#cost>50' -n 10");
    }
}
