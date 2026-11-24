package dev.pearlx.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The seven independence rules from the specification, exercised against the real core. */
class FeatureIndependenceTest {

    @TempDir
    Path directory;

    private FakePort port;
    private PearlXCore core;

    @BeforeEach
    void setUp() {
        port = new FakePort();
        core = new PearlXCore(port, directory, false);
        core.init();
        core.config().animationDelayValue = 1;
        core.config().animationDelayUnit = TimeUnit.SECONDS;
        core.config().animationDurationSeconds = 3;
        core.config().savingIntervalValue = 30;
        core.config().savingIntervalUnit = TimeUnit.SECONDS;
    }

    @AfterEach
    void tearDown() {
        core.shutdown();
    }

    private void throwPearlAndTick() {
        port.throwPearl("pearl", true, 0, 64, 0);
        playSeconds(0.05);
    }

    private void playSeconds(double seconds) {
        // Advance in client-tick sized steps so the animation produces real frames.
        int steps = Math.max(1, (int) Math.round(seconds / 0.05));
        for (int i = 0; i < steps; i++) {
            port.advanceSeconds(0.05);
            core.tick();
        }
    }

    @Test
    void disablingTrackingDoesNotDisableAnimations() {
        core.config().trackerEnabled = false;
        throwPearlAndTick();
        port.particles.clear();
        port.sounds.clear();

        playSeconds(1.2);

        assertEquals(0, core.trackedCount(), "the tracker is off");
        assertTrue(core.animations().triggerCount() > 0, "the animation still ran");
        assertFalse(port.sounds.isEmpty(), "the animation still played sound");
    }

    @Test
    void disablingAnimationsDoesNotDisableTracking() {
        core.config().animationEnabled = false;
        throwPearlAndTick();

        playSeconds(2.0);

        assertEquals(1, core.trackedCount(), "tracking keeps running");
        assertEquals(0, core.animations().runningSessions());
        assertTrue(port.particles.isEmpty(), "no animation particles");
        assertTrue(port.sounds.isEmpty(), "no animation sounds");
    }

    @Test
    void changingTheSavingTimerDoesNotChangeTheAnimationTimer() {
        core.config().savingIntervalValue = 1;
        core.config().savingIntervalUnit = TimeUnit.HOURS;
        throwPearlAndTick();
        playSeconds(1.2);
        int triggersWithOneHourSaving = core.animations().triggerCount();
        assertTrue(triggersWithOneHourSaving > 0);

        // Now change only Feature One's timer and run the same animation sequence again.
        core.config().savingIntervalValue = 2;
        core.config().savingIntervalUnit = TimeUnit.SECONDS;
        port.throwPearl("second", true, 1, 64, 1);
        playSeconds(1.2);

        assertEquals(triggersWithOneHourSaving + 1, core.animations().triggerCount(),
                "the animation still triggers exactly on its own delay");
        assertEquals(1_000L, core.config().animationDelayMillis());
    }

    @Test
    void changingTheAnimationTimerDoesNotChangeTheSavingTimer() {
        core.config().savingIntervalValue = 45;
        core.config().savingIntervalUnit = TimeUnit.SECONDS;
        long savingBefore = core.config().savingIntervalMillis();

        core.config().animationDelayValue = 5;
        core.config().animationDelayUnit = TimeUnit.MINUTES;

        assertEquals(savingBefore, core.config().savingIntervalMillis());
        assertEquals(45, core.config().savingIntervalValue);
        assertEquals(TimeUnit.SECONDS, core.config().savingIntervalUnit);
    }

    @Test
    void disablingAutomaticSavingDoesNotDisableTracking() {
        core.config().autoSaveEnabled = false;
        throwPearlAndTick();

        playSeconds(2.0);

        assertEquals(1, core.trackedCount());
        assertEquals(0, core.store().worldFileCount(), "nothing was written while auto save is off");
    }

    @Test
    void disablingSoundDoesNotDisableVisualEffects() {
        core.config().soundEffectsEnabled = false;
        throwPearlAndTick();
        port.particles.clear();
        port.sounds.clear();

        playSeconds(1.2);

        assertTrue(core.animations().runningSessions() > 0 || core.animations().triggerCount() > 0);
        assertFalse(port.particles.isEmpty(), "visual effects still run");
        assertTrue(port.sounds.isEmpty(), "no sounds");
    }

    @Test
    void disablingVisualEffectsDoesNotDisableSound() {
        core.config().visualEffectsEnabled = false;
        throwPearlAndTick();
        port.particles.clear();
        port.sounds.clear();

        playSeconds(1.2);

        assertTrue(core.animations().triggerCount() > 0);
        assertTrue(port.particles.isEmpty(), "no visual effects");
        assertFalse(port.sounds.isEmpty(), "sounds still play");
    }

    @Test
    void masterSwitchStopsEverything() {
        core.config().enabled = false;
        throwPearlAndTick();
        playSeconds(2.0);

        assertEquals(0, core.trackedCount());
        assertEquals(0, core.animations().triggerCount());
        assertTrue(port.particles.isEmpty());
        assertTrue(port.sounds.isEmpty());
    }

    @Test
    void switchingServersStopsOutdatedAnimations() {
        throwPearlAndTick();
        playSeconds(1.2);
        assertTrue(core.animations().runningSessions() > 0);

        port.world = WorldKey.server("another.example.com");
        core.tick();

        assertEquals(0, core.animations().runningSessions(), "no animation survives a server switch");
    }

    @Test
    void resettingRecordsKeepsConfigurationUntouched() {
        core.config().savingIntervalValue = 12;
        throwPearlAndTick();
        playSeconds(40);

        int removed = core.clearAllRecords();

        assertEquals(12, core.config().savingIntervalValue, "settings are not reset with the data");
        assertTrue(removed >= 0);
    }
}
