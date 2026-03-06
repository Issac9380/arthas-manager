package com.arthasmanager.arthas.command.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HeapDumpCommandTest {

    private HeapDumpCommand command;

    @BeforeEach
    void setUp() {
        command = new HeapDumpCommand();
    }

    @Test
    void getType_returnsHeapdump() {
        assertThat(command.getType()).isEqualTo("heapdump");
    }

    @Test
    void getDisplayName_returnsHeapDump() {
        assertThat(command.getDisplayName()).isEqualTo("Heap Dump");
    }

    @Test
    void getParams_returnsTwoParams() {
        assertThat(command.getParams()).hasSize(2);
        assertThat(command.getParams()).extracting("name")
                .containsExactly("filePath", "liveOnly");
    }

    @Test
    void buildCommandString_withNoParams_usesDefaultPathWithoutLive() {
        // boolVal returns false when key is absent → no --live flag
        String cmd = command.buildCommandString(new HashMap<>());
        assertThat(cmd).isEqualTo("heapdump /tmp/arthas-heapdump.hprof");
    }

    @Test
    void buildCommandString_withLiveOnlyTrue_appendsLiveFlag() {
        Map<String, Object> params = Map.of("liveOnly", "true");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("heapdump --live /tmp/arthas-heapdump.hprof");
    }

    @Test
    void buildCommandString_withLiveOnlyFalse_omitsLiveFlag() {
        Map<String, Object> params = Map.of("liveOnly", "false");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("heapdump /tmp/arthas-heapdump.hprof");
        assertThat(cmd).doesNotContain("--live");
    }

    @Test
    void buildCommandString_withCustomPath() {
        Map<String, Object> params = Map.of("filePath", "/data/heap.hprof");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("heapdump /data/heap.hprof");
    }

    @Test
    void buildCommandString_withCustomPathAndLive() {
        Map<String, Object> params = Map.of("filePath", "/data/heap.hprof", "liveOnly", "true");
        String cmd = command.buildCommandString(params);
        assertThat(cmd).isEqualTo("heapdump --live /data/heap.hprof");
    }
}
