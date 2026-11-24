package dev.pearlx.core;

/**
 * Feature One's location-saving timer.
 *
 * <p>Completely independent from {@link AnimationEngine}: it only knows the saving interval taken
 * from its own configuration fields.</p>
 */
public final class SaveScheduler {

    private long intervalMillis = -1L;
    private long nextDueAt;

    /** Applies the configured saving interval. Changing it never touches the animation timer. */
    public void reconfigure(PearlXConfig config, long now) {
        long configured = config.savingIntervalMillis();
        if (configured != intervalMillis || nextDueAt == 0L) {
            intervalMillis = configured;
            nextDueAt = now + configured;
        }
    }

    /** True at most once per configured interval. */
    public boolean poll(long now) {
        if (intervalMillis <= 0L) {
            return false;
        }
        if (now >= nextDueAt) {
            nextDueAt = now + intervalMillis;
            return true;
        }
        return false;
    }

    public void reset(long now) {
        nextDueAt = now + Math.max(intervalMillis, 1L);
    }

    public long intervalMillis() {
        return intervalMillis;
    }

    public long millisUntilNextSave(long now) {
        return Math.max(0L, nextDueAt - now);
    }
}
