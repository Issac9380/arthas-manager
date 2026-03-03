package com.arthasmanager.arthas.command.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardCommandTest {

    private DashboardCommand command;

    @BeforeEach
    void setUp() {
        command = new DashboardCommand();
    }

    @Test
    void getType_returnsDashboard() {
        assertThat(command.getType()).isEqualTo("dashboard");
    }

    @Test
    void getDisplayName_returnsDisplayName() {
        assertThat(command.getDisplayName()).isEqualTo("Dashboard");
    }

    @Test
    void getParams_returnsTwoParams() {
        assertThat(command.getParams()).hasSize(2);
        assertThat(command.getParams())
                .extracting("name")
                .containsExactly("interval", "count");
    }

    @Test
    void buildCommandString_withNoParams_usesDefaults() {
        String cmd = command.buildCommandString(new HashMap<>());

        assertThat(cmd).isEqualTo("dashboard -i 5000 -n 1");
    }

    @Test
    void buildCommandString_withCustomInterval() {
        Map<String, Object> params = Map.of("interval", "2000");

        String cmd = command.buildCommandString(params);

        assertThat(cmd).isEqualTo("dashboard -i 2000 -n 1");
    }

    @Test
    void buildCommandString_withCustomCount() {
        Map<String, Object> params = Map.of("count", "5");

        String cmd = command.buildCommandString(params);

        assertThat(cmd).isEqualTo("dashboard -i 5000 -n 5");
    }

    @Test
    void buildCommandString_withBothCustomParams() {
        Map<String, Object> params = Map.of("interval", "1000", "count", "3");

        String cmd = command.buildCommandString(params);

        assertThat(cmd).isEqualTo("dashboard -i 1000 -n 3");
    }

    @Test
    void buildCommandString_withInvalidInterval_usesDefault() {
        Map<String, Object> params = Map.of("interval", "notAnumber");

        String cmd = command.buildCommandString(params);

        assertThat(cmd).isEqualTo("dashboard -i 5000 -n 1");
    }

    @Test
    void buildCommandString_withNullParams_usesDefaults() {
        Map<String, Object> params = new HashMap<>();
        params.put("interval", null);
        params.put("count", null);

        String cmd = command.buildCommandString(params);

        assertThat(cmd).isEqualTo("dashboard -i 5000 -n 1");
    }
}
