package com.arthasmanager.arthas.command.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ThreadCommandTest {

    private ThreadCommand command;

    @BeforeEach
    void setUp() {
        command = new ThreadCommand();
    }

    @Test
    void getType_returnsThread() {
        assertThat(command.getType()).isEqualTo("thread");
    }

    @Test
    void getDisplayName_returnsThread() {
        assertThat(command.getDisplayName()).isEqualTo("Thread");
    }

    @Test
    void getParams_returnsThreeParams() {
        assertThat(command.getParams()).hasSize(3);
        assertThat(command.getParams()).extracting("name")
                .containsExactly("id", "top", "deadlock");
    }

    @Test
    void buildCommandString_withNoParams_returnsThreadOnly() {
        // str(params, "top") returns "" when absent → no -n flag appended
        String cmd = command.buildCommandString(new HashMap<>());
        assertThat(cmd).isEqualTo("thread");
    }

    @Test
    void buildCommandString_withDefaultTop_usesDefaultValue() {
        Map<String, Object> params = Map.of("top", "5");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("thread -n 5");
    }

    @Test
    void buildCommandString_withThreadId_returnsSingleThreadTrace() {
        String cmd = command.buildCommandString(Map.of("id", "42"));
        assertThat(cmd).isEqualTo("thread 42");
    }

    @Test
    void buildCommandString_withDeadlock_returnsDeadlockCommand() {
        String cmd = command.buildCommandString(Map.of("deadlock", "true"));
        assertThat(cmd).isEqualTo("thread -b");
    }

    @Test
    void buildCommandString_withCustomTop_usesCustomN() {
        String cmd = command.buildCommandString(Map.of("top", "10"));
        assertThat(cmd).isEqualTo("thread -n 10");
    }

    @Test
    void buildCommandString_idTakesPriorityOverDeadlock() {
        String cmd = command.buildCommandString(Map.of("id", "7", "deadlock", "true"));
        assertThat(cmd).isEqualTo("thread 7");
    }

    @Test
    void buildCommandString_withEmptyTop_usesDefaultTop() {
        Map<String, Object> params = new HashMap<>();
        params.put("top", "");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("thread");
    }
}
