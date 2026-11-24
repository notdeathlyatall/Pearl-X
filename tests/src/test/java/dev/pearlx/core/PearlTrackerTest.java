package dev.pearlx.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Feature One: detection, ownership, multiple pearls, clean-up and persistence. */
class PearlTrackerTest {

    @TempDir
    Path directory;

    private FakePort port;
    private PearlXConfig config;
    private RecordStore store;
    private PearlTracker tracker;

    @BeforeEach
    void setUp() {
        port = new FakePort();
        config = new PearlXConfig();
        store = new RecordStore(directory, false);
        tracker = new PearlTracker(store);
        tracker.switchWorld(port.currentWorldKey(), config);
    }

    @Test
    void tracksOnlyPearlsOwnedByTheLocalPlayer() {
        port.throwPearl("mine", true, 1, 64, 1);
        port.throwPearl("somebody-else", false, 8, 64, 8);

        tracker.tick(port, config, port.scanPearls(), port.now);

        assertEquals(1, tracker.size());
        assertEquals("mine", tracker.allPearls().get(0).id());
    }

    @Test
    void tracksSeveralPearlsIndependently() {
        port.throwPearl("one", true, 0, 64, 0);
        port.throwPearl("two", true, 10, 64, 10);
        port.throwPearl("three", true, 20, 64, 20);

        tracker.tick(port, config, port.scanPearls(), port.now);
        assertEquals(3, tracker.size());

        port.movePearl("two", 12, 64, 12);
        port.removePearl("one");
        port.advanceSeconds(1);
        tracker.tick(port, config, port.scanPearls(), port.now);

        assertEquals(2, tracker.size());
        TrackedPearl remaining = tracker.allPearls().stream()
                .filter(pearl -> pearl.id().equals("two")).findFirst().orElseThrow();
        assertEquals(12.0, remaining.x(), 0.001);
    }

    @Test
    void removesRecordWhenPearlLandsInALoadedChunk() {
        port.throwPearl("pearl", true, 3, 64, 3);
        tracker.tick(port, config, port.scanPearls(), port.now);
        assertEquals(1, tracker.size());

        port.removePearl("pearl");
        tracker.tick(port, config, port.scanPearls(), port.now + 50);

        assertEquals(0, tracker.size(), "a landed or retrieved pearl is cleaned up immediately");
    }

    @Test
    void keepsRecordWhenTheChunkIsNotLoaded() {
        port.throwPearl("pearl", true, 500, 64, 500);
        tracker.tick(port, config, port.scanPearls(), port.now);

        port.chunksLoaded = false;
        port.removePearl("pearl");
        tracker.tick(port, config, port.scanPearls(), port.now + 50);

        assertEquals(1, tracker.size(), "an unloaded pearl must not be deleted by mistake");
        assertEquals(PearlStatus.UNLOADED, tracker.allPearls().get(0).status());
    }

    @Test
    void unloadedRecordExpiresAfterTheConfiguredTime() {
        config.staleRecordMinutes = 1;
        port.throwPearl("pearl", true, 500, 64, 500);
        tracker.tick(port, config, port.scanPearls(), port.now);

        port.chunksLoaded = false;
        port.removePearl("pearl");
        tracker.tick(port, config, port.scanPearls(), port.now + 1_000L);
        assertEquals(1, tracker.size());

        // The one-minute lifetime starts when the pearl became unloaded, so it expires after that.
        tracker.tick(port, config, port.scanPearls(), port.now + 62_000L);
        assertEquals(0, tracker.size());
    }

    @Test
    void reobservedPearlDoesNotCreateDuplicates() {
        port.throwPearl("pearl", true, 0, 64, 0);
        for (int i = 0; i < 20; i++) {
            port.advanceSeconds(0.05);
            tracker.tick(port, config, port.scanPearls(), port.now);
        }
        assertEquals(1, tracker.size());
    }

    @Test
    void recordsSurviveARestart() {
        port.throwPearl("pearl", true, 5, 70, -5);
        tracker.tick(port, config, port.scanPearls(), port.now);
        assertTrue(tracker.flush(config));
        assertTrue(Files.isRegularFile(directory.resolve(port.world.fileName())));

        PearlTracker restarted = new PearlTracker(new RecordStore(directory, false));
        restarted.switchWorld(port.currentWorldKey(), config);

        assertEquals(1, restarted.size());
        TrackedPearl restored = restarted.allPearls().get(0);
        assertEquals(PearlStatus.RESTORED, restored.status());
        assertFalse(restored.sessionPearl(), "restored pearls never replay their animation");
    }

    @Test
    void disablingTrackingKeepsStoredLocations() throws IOException {
        port.throwPearl("pearl", true, 1, 64, 1);
        tracker.tick(port, config, port.scanPearls(), port.now);
        tracker.flush(config);

        config.trackerEnabled = false;
        port.advanceSeconds(2);
        tracker.tick(port, config, port.scanPearls(), port.now);

        assertEquals(0, tracker.size(), "tracking stops");
        assertTrue(Files.isRegularFile(directory.resolve(port.world.fileName())),
                "stored locations must not be deleted");
    }

    @Test
    void serverTrackingSwitchIsRespected() {
        config.trackServers = false;
        port.throwPearl("pearl", true, 1, 64, 1);

        tracker.tick(port, config, port.scanPearls(), port.now);

        assertEquals(0, tracker.size());
    }
}
