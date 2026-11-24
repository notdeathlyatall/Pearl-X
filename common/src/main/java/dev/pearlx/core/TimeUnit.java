package dev.pearlx.core;

/**
 * Time units selectable independently for the location-saving timer and for the animation
 * trigger delay. They are shared vocabulary only: the two timers never share a configuration.
 */
public enum TimeUnit {
    SECONDS("sec", 1_000L, 60),
    MINUTES("min", 60_000L, 60),
    HOURS("hr", 3_600_000L, 24);

    private final String label;
    private final long millis;
    private final int maxValue;

    TimeUnit(String label, long millis, int maxValue) {
        this.label = label;
        this.millis = millis;
        this.maxValue = maxValue;
    }

    public String label() {
        return label;
    }

    /** Largest value the GUI and validation accept for this unit. */
    public int maxValue() {
        return maxValue;
    }

    public long toMillis(int value) {
        return Math.max(1, value) * millis;
    }

    public TimeUnit cycle(int direction) {
        TimeUnit[] values = values();
        return values[Math.floorMod(ordinal() + direction, values.length)];
    }

    public static TimeUnit fromName(String name, TimeUnit fallback) {
        if (name != null) {
            for (TimeUnit unit : values()) {
                if (unit.name().equalsIgnoreCase(name)) {
                    return unit;
                }
            }
        }
        return fallback;
    }
}
