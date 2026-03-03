package com.arthasmanager.arthas.command.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WatchCommandTest {

    private WatchCommand command;

    @BeforeEach
    void setUp() {
        command = new WatchCommand();
    }

    @Test
    void getType_returnsWatch() {
        assertThat(command.getType()).isEqualTo("watch");
    }

    @Test
    void getParams_returnsEightParams() {
        assertThat(command.getParams()).hasSize(8);
    }

    @Test
    void buildCommandString_minimalRequiredParams() {
        Map<String, Object> params = Map.of(
                "className", "com.example.UserService",
                "methodName", "getUser"
        );

        String cmd = command.buildCommandString(params);

        assertThat(cmd)
                .startsWith("watch com.example.UserService getUser")
                .contains("\"{params, returnObj, throwExp}\"")
                .contains("-n 5")
                .contains("-x 1");
    }

    @Test
    void buildCommandString_withCondition_appendsCondition() {
        Map<String, Object> params = Map.of(
                "className", "com.example.OrderService",
                "methodName", "processOrder",
                "condition", "params[0]>100"
        );

        String cmd = command.buildCommandString(params);

        assertThat(cmd).contains("'params[0]>100'");
    }

    @Test
    void buildCommandString_withNoCondition_omitsConditionClause() {
        Map<String, Object> params = Map.of(
                "className", "com.example.Foo",
                "methodName", "bar"
        );

        String cmd = command.buildCommandString(params);

        assertThat(cmd).doesNotContain("'");
    }

    @Test
    void buildCommandString_beforeInvokeTrue_appendsDashB() {
        Map<String, Object> params = new HashMap<>();
        params.put("className", "com.example.Svc");
        params.put("methodName", "call");
        params.put("beforeInvoke", "true");

        String cmd = command.buildCommandString(params);

        assertThat(cmd).contains("-b");
    }

    @Test
    void buildCommandString_beforeInvokeFalse_noDashB() {
        Map<String, Object> params = new HashMap<>();
        params.put("className", "com.example.Svc");
        params.put("methodName", "call");
        params.put("beforeInvoke", "false");

        String cmd = command.buildCommandString(params);

        assertThat(cmd).doesNotContain("-b");
    }

    @Test
    void buildCommandString_onExceptionTrue_appendsDashE() {
        Map<String, Object> params = new HashMap<>();
        params.put("className", "com.example.Svc");
        params.put("methodName", "call");
        params.put("onException", "true");

        String cmd = command.buildCommandString(params);

        assertThat(cmd).contains("-e");
    }

    @Test
    void buildCommandString_customCountAndExpand() {
        Map<String, Object> params = Map.of(
                "className", "com.example.Svc",
                "methodName", "call",
                "count", "10",
                "expand", "3"
        );

        String cmd = command.buildCommandString(params);

        assertThat(cmd).contains("-n 10").contains("-x 3");
    }

    @Test
    void buildCommandString_customExpression() {
        Map<String, Object> params = Map.of(
                "className", "com.example.Svc",
                "methodName", "call",
                "express", "returnObj"
        );

        String cmd = command.buildCommandString(params);

        assertThat(cmd).contains("\"returnObj\"");
    }

    @Test
    void buildCommandString_invalidCount_usesDefault() {
        Map<String, Object> params = new HashMap<>();
        params.put("className", "com.example.Svc");
        params.put("methodName", "call");
        params.put("count", "abc");

        String cmd = command.buildCommandString(params);

        assertThat(cmd).contains("-n 5");
    }
}
