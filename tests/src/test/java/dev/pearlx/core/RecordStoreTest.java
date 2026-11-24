package dev.pearlx.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Local storage: separate files per server/world, no pointless writes, scoped resets. */
class RecordStoreTest {

    @TempDir
    Path directory;

    private RecordStore store;
    private WorldKey server;
    private WorldKey world;

    @BeforeEach
    void setUp() {
        store = new RecordStore(directory, false);
        server = WorldKey.server("play.example.com");
        world = WorldKey.singleplayer("Survival World");
    }

    private static StoredPearl pearl(String id) {
        return new StoredPearl(id, 1.5, 64.0, -2.5, "minecraft:overworld", 1L, 2L, "ACTIVE");
    }

    @Test
    void serversAndWorldsUseSeparateFiles() {
        assertTrue(store.saveIfChanged(server, List.of(pearl("a"))));
        assertTrue(store.saveIfChanged(world, List.of(pearl("b"), pearl("c"))));

        assertEquals(2, store.storedWorlds().size());
        assertEquals(1, store.load(server).size());
        assertEquals(2, store.load(world).size());
        assertTrue(Files.isRegularFile(directory.resolve(server.fileName())));
    }

    @Test
    void unchangedRecordsAreNotRewritten() {
        List<StoredPearl> records = List.of(pearl("a"));
        assertTrue(store.saveIfChanged(server, records));
        assertFalse(store.saveIfChanged(server, List.of(pearl("a"))),
                "identical data must not touch the disk again");

        assertTrue(store.saveIfChanged(server, List.of(pearl("a"), pearl("b"))));
    }

    @Test
    void emptyRecordSetRemovesTheFile() {
        assertTrue(store.saveIfChanged(server, List.of(pearl("a"))));
        assertTrue(Files.isRegularFile(directory.resolve(server.fileName())));

        assertTrue(store.saveIfChanged(server, List.of()));
        assertFalse(Files.exists(directory.resolve(server.fileName())));
    }

    @Test
    void resettingAllServersKeepsSingleplayerWorlds() {
        store.saveIfChanged(server, List.of(pearl("a")));
        store.saveIfChanged(WorldKey.server("other.example.com"), List.of(pearl("b")));
        store.saveIfChanged(world, List.of(pearl("c")));

        assertEquals(2, store.deleteWhere(WorldKey::isServer));
        assertEquals(1, store.storedWorlds().size());
        assertTrue(store.storedWorlds().contains(world));
    }

    @Test
    void resettingAllWorldsKeepsServers() {
        store.saveIfChanged(server, List.of(pearl("a")));
        store.saveIfChanged(world, List.of(pearl("c")));

        assertEquals(1, store.deleteWhere(WorldKey::isSingleplayer));
        assertTrue(store.storedWorlds().contains(server));
    }

    @Test
    void clearAllRemovesEverything() {
        store.saveIfChanged(server, List.of(pearl("a")));
        store.saveIfChanged(world, List.of(pearl("c")));

        assertEquals(2, store.clearAll());
        assertEquals(0, store.storedWorlds().size());
    }

    @Test
    void loadsNothingForUnknownWorlds() {
        assertTrue(store.load(WorldKey.server("never-played")).isEmpty());
    }

    @Test
    void worldKeysSurviveAFileRoundTrip() {
        store.saveIfChanged(world, List.of(pearl("a")));
        WorldKey parsed = WorldKey.parse("singleplayer:Survival World");
        assertEquals(world, parsed);
        assertEquals(1, store.load(parsed).size());
    }
}
