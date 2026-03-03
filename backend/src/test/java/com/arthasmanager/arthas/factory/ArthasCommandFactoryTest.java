package com.arthasmanager.arthas.factory;

import com.arthasmanager.arthas.command.ArthasCommand;
import com.arthasmanager.arthas.command.CommandParam;
import com.arthasmanager.arthas.command.impl.DashboardCommand;
import com.arthasmanager.arthas.command.impl.WatchCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArthasCommandFactoryTest {

    private ArthasCommandFactory factory;

    @BeforeEach
    void setUp() {
        List<ArthasCommand> commands = List.of(new DashboardCommand(), new WatchCommand());
        factory = new ArthasCommandFactory(commands);
    }

    @Test
    void getCommand_knownType_returnsCommandInstance() {
        ArthasCommand cmd = factory.getCommand("dashboard");

        assertThat(cmd).isInstanceOf(DashboardCommand.class);
    }

    @Test
    void getCommand_anotherKnownType_returnsWatchCommand() {
        ArthasCommand cmd = factory.getCommand("watch");

        assertThat(cmd).isInstanceOf(WatchCommand.class);
    }

    @Test
    void getCommand_unknownType_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> factory.getCommand("nonexistent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nonexistent");
    }

    @Test
    void listCommandMeta_returnsMetaForAllCommands() {
        List<Map<String, Object>> meta = factory.listCommandMeta();

        assertThat(meta).hasSize(2);
        assertThat(meta).extracting(m -> m.get("type"))
                .containsExactlyInAnyOrder("dashboard", "watch");
    }

    @Test
    void listCommandMeta_eachEntryHasRequiredKeys() {
        List<Map<String, Object>> meta = factory.listCommandMeta();

        for (Map<String, Object> entry : meta) {
            assertThat(entry).containsKeys("type", "displayName", "description", "params");
        }
    }

    @Test
    void listCommandMeta_paramsAreListOfMaps() {
        List<Map<String, Object>> meta = factory.listCommandMeta();

        Map<String, Object> dashboardMeta = meta.stream()
                .filter(m -> "dashboard".equals(m.get("type")))
                .findFirst().orElseThrow();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> params = (List<Map<String, Object>>) dashboardMeta.get("params");

        assertThat(params).hasSize(2);
        assertThat(params.get(0)).containsKeys("name", "label", "type", "required", "description");
    }

    @Test
    void listCommandMeta_paramDefaultValueIncludedWhenPresent() {
        List<Map<String, Object>> meta = factory.listCommandMeta();

        Map<String, Object> watchMeta = meta.stream()
                .filter(m -> "watch".equals(m.get("type")))
                .findFirst().orElseThrow();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> params = (List<Map<String, Object>>) watchMeta.get("params");

        Map<String, Object> expressParam = params.stream()
                .filter(p -> "express".equals(p.get("name")))
                .findFirst().orElseThrow();

        assertThat(expressParam.get("defaultValue")).isEqualTo("{params, returnObj, throwExp}");
    }

    @Test
    void factory_withEmptyCommandList_listMetaReturnsEmpty() {
        ArthasCommandFactory emptyFactory = new ArthasCommandFactory(List.of());

        assertThat(emptyFactory.listCommandMeta()).isEmpty();
    }

    @Test
    void factory_withEmptyCommandList_getCommandThrows() {
        ArthasCommandFactory emptyFactory = new ArthasCommandFactory(List.of());

        assertThatThrownBy(() -> emptyFactory.getCommand("dashboard"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
