package com.arthasmanager.arthas.command.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OgnlCommandTest {

    private OgnlCommand command;

    @BeforeEach
    void setUp() {
        command = new OgnlCommand();
    }

    @Test
    void getType_returnsOgnl() {
        assertThat(command.getType()).isEqualTo("ognl");
    }

    @Test
    void getDisplayName_returnsOgnlExpression() {
        assertThat(command.getDisplayName()).isEqualTo("OGNL Expression");
    }

    @Test
    void getParams_returnsTwoParams() {
        assertThat(command.getParams()).hasSize(2);
        assertThat(command.getParams()).extracting("name")
                .containsExactly("expression", "expand");
    }

    @Test
    void buildCommandString_withExpression_usesDefaultExpand() {
        Map<String, Object> params = Map.of("expression", "@System@getProperty(\"java.version\")");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("ognl '@System@getProperty(\"java.version\")' -x 1");
    }

    @Test
    void buildCommandString_withCustomExpand() {
        Map<String, Object> params = Map.of("expression", "#root", "expand", "3");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("ognl '#root' -x 3");
    }

    @Test
    void buildCommandString_withExpandZero() {
        Map<String, Object> params = Map.of("expression", "1+1", "expand", "0");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("ognl '1+1' -x 0");
    }

    @Test
    void buildCommandString_withInvalidExpand_usesDefault() {
        Map<String, Object> params = Map.of("expression", "true", "expand", "notANumber");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("ognl 'true' -x 1");
    }
}
