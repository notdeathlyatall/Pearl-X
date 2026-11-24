package dev.pearlx.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Feature Two: trigger timing, duplicates, multiple pearls, completion and cleanup. */
class AnimationEngineTest {

    private FakePort port;
    private PearlXConfig config;
    private AnimationEngine engine;

    @BeforeEach
    void setUp() {
        port = new FakePort();
        config = new PearlXConfig();
        config.animationDelayValue = 1;
        config.animationDelayUnit = TimeUnit.SECONDS;
        config.animationDurationSeconds = 2;
        engine = new AnimationEngine();
    }

    private void tick(double secondsFromStart) {
        engine.tick(port, config, port.scanPearls(), port.now + (long) (secondsFromStart * 1000.0));
    }

    @Test
    void waitsForItsOwnTriggerDelay() {
        port.throwPearl("pearl", true, 0, 64, 0);
        tick(0.0);
        tick(0.9);
        assertEquals(0, engine.runningSessions());

        tick(1.0);
        assertEquals(1, engine.runningSessions());
        assertEquals(1, engine.triggerCount());
    }

    @Test
    void neverPlaysTwiceForTheSamePearl() {
        port.throwPearl("pearl", true, 0, 64, 0);
        for (int i = 0; i <= 40; i++) {
            tick(i * 0.05);
        }
        assertEquals(1, engine.triggerCount(), "one trigger per pearl");
        long startSounds = port.sounds.stream().filter(cue -> cue == SoundCue.TRAVEL_START).count();
        assertEquals(1, startSounds);
    }

    @Test
    void finishesAndPlaysTheEndSound() {
        port.throwPearl("pearl", true, 0, 64, 0);
        tick(0.0);
        tick(1.0);
        assertEquals(1, engine.runningSessions());

        tick(3.1);

        assertEquals(0, engine.runningSessions(), "completed sessions are cleaned up");
        assertTrue(port.sounds.contains(SoundCue.TRAVEL_END));
    }

    @Test
    void animatesSeveralPearlsIndependently() {
        port.throwPearl("one", true, 0, 64, 0);
        port.throwPearl("two", true, 5, 64, 5);
        tick(0.0);
        tick(1.0);

        assertEquals(2, engine.runningSessions());
        assertEquals(2, engine.triggerCount());
    }

    @Test
    void cancelsWhenThePearlDisappears() {
        port.throwPearl("pearl", true, 0, 64, 0);
        tick(0.0);
        tick(1.0);
        assertEquals(1, engine.runningSessions());

        port.removePearl("pearl");
        tick(1.2);

        assertEquals(0, engine.runningSessions(), "no animation plays for a pearl that is gone");
    }

    @Test
    void ignoresPearlsOwnedByOtherPlayers() {
        port.throwPearl("other", false, 0, 64, 0);
        tick(0.0);
        tick(2.0);

        assertEquals(0, engine.triggerCount());
        assertTrue(port.particles.isEmpty());
    }

    @Test
    void clearingStopsEverything() {
        port.throwPearl("pearl", true, 0, 64, 0);
        tick(0.0);
        tick(1.0);
        engine.clear();

        assertEquals(0, engine.runningSessions());
    }

    @Test
    void zeroVolumePlaysNothing() {
        config.soundVolumePercent = 0;
        port.throwPearl("pearl", true, 0, 64, 0);
        tick(0.0);
        tick(1.0);
        tick(3.2);

        assertTrue(port.sounds.isEmpty(), "0% volume must stay silent");
        assertFalse(port.particles.isEmpty(), "visuals still run");
    }
}
