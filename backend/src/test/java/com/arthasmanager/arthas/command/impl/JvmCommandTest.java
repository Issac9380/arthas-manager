package com.arthasmanager.arthas.command.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JvmCommandTest {

    private JvmCommand command;

    @BeforeEach
    void setUp() {
        command = new JvmCommand();
    }

    @Test
    void getType_returnsJvm() {
        assertThat(command.getType()).isEqualTo("jvm");
    }

    @Test
    void getDisplayName_returnsJvmInfo() {
        assertThat(command.getDisplayName()).isEqualTo("JVM Info");
    }

    @Test
    void getParams_returnsEmptyList() {
        assertThat(command.getParams()).isEmpty();
    }

    @Test
    void buildCommandString_withEmptyParams_returnsJvm() {
        assertThat(command.buildCommandString(new HashMap<>())).isEqualTo("jvm");
    }

    @Test
    void buildCommandString_ignoresAnyParams() {
        assertThat(command.buildCommandString(Map.of("foo", "bar"))).isEqualTo("jvm");
    }
}
