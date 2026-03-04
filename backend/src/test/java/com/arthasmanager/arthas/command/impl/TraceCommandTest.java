package com.arthasmanager.arthas.command.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TraceCommandTest {

    private TraceCommand command;

    @BeforeEach
    void setUp() {
        command = new TraceCommand();
    }

    @Test
    void getType_returnsTrace() {
        assertThat(command.getType()).isEqualTo("trace");
    }

    @Test
    void getDisplayName_returnsTrace() {
        assertThat(command.getDisplayName()).isEqualTo("Trace");
    }

    @Test
    void getParams_returnsFiveParams() {
        assertThat(command.getParams()).hasSize(5);
        assertThat(command.getParams()).extracting("name")
                .containsExactly("className", "methodName", "condition", "count", "skipJdk");
    }

    @Test
    void buildCommandString_minimal_noSkipJdk() {
        // boolVal returns false when skipJdk absent → no --skipJDKMethod flag
        Map<String, Object> params = Map.of(
                "className", "com.example.Foo",
                "methodName", "bar");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("trace com.example.Foo bar -n 5");
    }

    @Test
    void buildCommandString_withSkipJdkTrue_appendsFlag() {
        Map<String, Object> params = Map.of(
                "className", "com.example.Foo",
                "methodName", "bar",
                "skipJdk", "true");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("trace com.example.Foo bar -n 5 --skipJDKMethod true");
    }

    @Test
    void buildCommandString_withCondition_appendsCondition() {
        Map<String, Object> params = Map.of(
                "className", "com.example.Foo",
                "methodName", "bar",
                "condition", "#cost>10",
                "skipJdk", "true");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("trace com.example.Foo bar '#cost>10' -n 5 --skipJDKMethod true");
    }

    @Test
    void buildCommandString_withCustomCount() {
        Map<String, Object> params = Map.of(
                "className", "com.example.Foo",
                "methodName", "bar",
                "count", "20",
                "skipJdk", "true");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("trace com.example.Foo bar -n 20 --skipJDKMethod true");
    }

    @Test
    void buildCommandString_withSkipJdkFalse_omitsFlag() {
        Map<String, Object> params = Map.of(
                "className", "com.example.Foo",
                "methodName", "bar",
                "skipJdk", "false");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("trace com.example.Foo bar -n 5");
        assertThat(cmd).doesNotContain("--skipJDKMethod");
    }

    @Test
    void buildCommandString_withAllParams() {
        Map<String, Object> params = Map.of(
                "className", "com.example.OrderService",
                "methodName", "createOrder",
                "condition", "#cost>100",
                "count", "3",
                "skipJdk", "true");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("trace com.example.OrderService createOrder '#cost>100' -n 3 --skipJDKMethod true");
    }
}
