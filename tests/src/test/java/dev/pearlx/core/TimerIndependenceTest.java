package dev.pearlx.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The two features own separate timers; changing one must never move the other. */
class TimerIndependenceTest {

    @Test
    void savingIntervalChangeDoesNotTouchAnimationDelay() {
        PearlXConfig config = new PearlXConfig();
        config.animationDelayValue = 7;
        config.animationDelayUnit = TimeUnit.SECONDS;
        long animationBefore = config.animationDelayMillis();

        config.savingIntervalValue = 2;
        config.savingIntervalUnit = TimeUnit.HOURS;

        assertEquals(animationBefore, config.animationDelayMillis());
        assertEquals(TimeUnit.SECONDS, config.animationDelayUnit);
        assertEquals(7, config.animationDelayValue);
        assertEquals(2 * 3_600_000L, config.savingIntervalMillis());
    }

    @Test
    void animationDelayChangeDoesNotTouchSavingInterval() {
        PearlXConfig config = new PearlXConfig();
        config.savingIntervalValue = 30;
        config.savingIntervalUnit = TimeUnit.SECONDS;
        long savingBefore = config.savingIntervalMillis();

        config.animationDelayValue = 3;
        config.animationDelayUnit = TimeUnit.MINUTES;

        assertEquals(savingBefore, config.savingIntervalMillis());
        assertEquals(30, config.savingIntervalValue);
        assertEquals(TimeUnit.SECONDS, config.savingIntervalUnit);
        assertEquals(180_000L, config.animationDelayMillis());
    }

    @Test
    void unitsAreConvertedIndependently() {
        PearlXConfig config = new PearlXConfig();
        assertEquals(TimeUnit.values().length, 3);
        config.savingIntervalValue = 5;
        config.savingIntervalUnit = TimeUnit.SECONDS;
        config.animationDelayValue = 2;
        config.animationDelayUnit = TimeUnit.HOURS;

        assertEquals(5_000L, config.savingIntervalMillis());
        assertEquals(7_200_000L, config.animationDelayMillis());
        assertNotEquals(config.savingIntervalMillis(), config.animationDelayMillis());
    }

    @Test
    void validationOnlyClampsTheEditedTimer() {
        PearlXConfig config = new PearlXConfig();
        config.savingIntervalValue = 30;
        config.savingIntervalUnit = TimeUnit.SECONDS;
        config.animationDelayUnit = TimeUnit.MINUTES;
        config.animationDelayValue = 9999;

        config.validate();

        assertEquals(60, config.animationDelayValue, "minutes are limited to 60");
        assertEquals(30, config.savingIntervalValue, "the saving value is untouched");
    }

    @Test
    void saveSchedulerFiresOnItsOwnInterval() {
        SaveScheduler scheduler = new SaveScheduler();
        PearlXConfig config = new PearlXConfig();
        config.savingIntervalValue = 5;
        config.savingIntervalUnit = TimeUnit.SECONDS;

        long now = 1_000L;
        scheduler.reconfigure(config, now);
        assertTrue(!scheduler.poll(now + 4_999L));
        assertTrue(scheduler.poll(now + 5_000L));
        assertTrue(!scheduler.poll(now + 9_999L));
        assertTrue(scheduler.poll(now + 10_000L));
    }

    @Test
    void animationEngineIgnoresTheSavingTimer() {
        FakePort port = new FakePort();
        PearlXConfig config = new PearlXConfig();
        config.savingIntervalValue = 1;
        config.savingIntervalUnit = TimeUnit.HOURS;
        config.animationDelayValue = 2;
        config.animationDelayUnit = TimeUnit.SECONDS;
        config.animationDurationSeconds = 1;

        AnimationEngine engine = new AnimationEngine();
        port.throwPearl("pearl-1", true, 0, 64, 0);

        engine.tick(port, config, port.scanPearls(), port.now);
        assertEquals(0, engine.runningSessions(), "animation must wait for its own delay");

        // Two seconds after the pearl was first observed, the animation's own timer has elapsed.
        engine.tick(port, config, port.scanPearls(), port.now + 2_000L);
        assertEquals(1, engine.runningSessions(), "animation starts on its own timer");
    }
}
