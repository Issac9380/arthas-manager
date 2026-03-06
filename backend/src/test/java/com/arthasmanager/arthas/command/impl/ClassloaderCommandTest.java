package com.arthasmanager.arthas.command.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ClassloaderCommandTest {

    private ClassloaderCommand command;

    @BeforeEach
    void setUp() {
        command = new ClassloaderCommand();
    }

    @Test
    void getType_returnsClassloader() {
        assertThat(command.getType()).isEqualTo("classloader");
    }

    @Test
    void getDisplayName_returnsClassloader() {
        assertThat(command.getDisplayName()).isEqualTo("Classloader");
    }

    @Test
    void getParams_returnsTwoParams() {
        assertThat(command.getParams()).hasSize(2);
        assertThat(command.getParams()).extracting("name")
                .containsExactly("tree", "stats");
    }

    @Test
    void buildCommandString_withNoFlags_returnsClassloaderOnly() {
        String cmd = command.buildCommandString(new HashMap<>());
        assertThat(cmd).isEqualTo("classloader");
    }

    @Test
    void buildCommandString_withTreeTrue_appendsT() {
        Map<String, Object> params = Map.of("tree", "true");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("classloader -t");
    }

    @Test
    void buildCommandString_withStatsTrue_appendsL() {
        Map<String, Object> params = Map.of("stats", "true");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("classloader -l");
    }

    @Test
    void buildCommandString_withBothFlags_appendsBoth() {
        Map<String, Object> params = Map.of("tree", "true", "stats", "true");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("classloader -t -l");
    }

    @Test
    void buildCommandString_withBothFalse_returnsClassloaderOnly() {
        Map<String, Object> params = Map.of("tree", "false", "stats", "false");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("classloader");
    }
}
