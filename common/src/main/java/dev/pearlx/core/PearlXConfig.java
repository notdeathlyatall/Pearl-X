package dev.pearlx.core;

import com.google.gson.annotations.SerializedName;

/**
 * The complete Pearl X configuration.
 *
 * <p>Feature One (Pearl Location Tracker) and Feature Two (Universal Pearl Travel Animation) hold
 * completely separate fields, including their own timer value and time unit. No helper in this
 * class mixes the two, and no timer reads the other feature's fields.</p>
 */
public final class PearlXConfig {

    public static final int SAVING_INTERVAL_DEFAULT_VALUE = 30;
    public static final int ANIMATION_DELAY_DEFAULT_VALUE = 1;
    public static final int ANIMATION_DURATION_DEFAULT_SECONDS = 2;
    public static final int SOUND_VOLUME_DEFAULT_PERCENT = 70;
    public static final int STALE_RECORD_DEFAULT_MINUTES = 30;

    // ------------------------------------------------------------------ General
    /** Master switch. When false every part of the mod stops. */
    @SerializedName("enabled")
    public boolean enabled = true;

    /** Feature One master switch. Never touches the animation feature. */
    @SerializedName("tracker_enabled")
    public boolean trackerEnabled = true;

    /** Feature Two master switch. Never touches location tracking. */
    @SerializedName("animation_enabled")
    public boolean animationEnabled = true;

    // ------------------------------------------------------- Feature One: tracker
    @SerializedName("detection_enabled")
    public boolean detectionEnabled = true;

    /** Independent control for periodic saves. Tracking keeps running when this is off. */
    @SerializedName("auto_save_enabled")
    public boolean autoSaveEnabled = true;

    /** Location-saving timer value. Belongs to Feature One only. */
    @SerializedName("saving_interval_value")
    public int savingIntervalValue = SAVING_INTERVAL_DEFAULT_VALUE;

    /** Location-saving timer unit. Belongs to Feature One only. */
    @SerializedName("saving_interval_unit")
    public TimeUnit savingIntervalUnit = TimeUnit.SECONDS;

    @SerializedName("track_servers")
    public boolean trackServers = true;

    @SerializedName("track_singleplayer")
    public boolean trackSingleplayer = true;

    /** How long a record whose pearl is in an unloaded chunk is kept before it expires. */
    @SerializedName("stale_record_minutes")
    public int staleRecordMinutes = STALE_RECORD_DEFAULT_MINUTES;

    // ----------------------------------------------- Feature Two: travel animation
    @SerializedName("visual_effects_enabled")
    public boolean visualEffectsEnabled = true;

    /** Animation timer value. Belongs to Feature Two only. */
    @SerializedName("animation_delay_value")
    public int animationDelayValue = ANIMATION_DELAY_DEFAULT_VALUE;

    /** Animation timer unit. Belongs to Feature Two only. */
    @SerializedName("animation_delay_unit")
    public TimeUnit animationDelayUnit = TimeUnit.SECONDS;

    @SerializedName("animation_duration_seconds")
    public int animationDurationSeconds = ANIMATION_DURATION_DEFAULT_SECONDS;

    @SerializedName("sound_effects_enabled")
    public boolean soundEffectsEnabled = true;

    @SerializedName("sound_volume_percent")
    public int soundVolumePercent = SOUND_VOLUME_DEFAULT_PERCENT;

    // -------------------------------------------------------------- Advanced
    @SerializedName("debug_enabled")
    public boolean debugEnabled = false;

    @SerializedName("show_tracking_info")
    public boolean showTrackingInfo = false;

    // ------------------------------------------------------------------ derived

    /** Feature One's timer in milliseconds. */
    public long savingIntervalMillis() {
        return savingIntervalUnit.toMillis(clamp(savingIntervalValue, 1, savingIntervalUnit.maxValue()));
    }

    /** Feature Two's timer in milliseconds. */
    public long animationDelayMillis() {
        return animationDelayUnit.toMillis(clamp(animationDelayValue, 1, animationDelayUnit.maxValue()));
    }

    public long animationDurationMillis() {
        return clamp(animationDurationSeconds, 1, 15) * 1_000L;
    }

    public float soundVolume() {
        return clamp(soundVolumePercent, 0, 100) / 100.0F;
    }

    public long staleRecordMillis() {
        return clamp(staleRecordMinutes, 1, 720) * 60_000L;
    }

    /** Whether Feature One is allowed to track the given world kind at all. */
    public boolean trackingAllowedFor(boolean server) {
        return server ? trackServers : trackSingleplayer;
    }

    public boolean trackingActive() {
        return enabled && trackerEnabled && detectionEnabled;
    }

    public boolean animationActive() {
        return enabled && animationEnabled;
    }

    /** Clamps every value that can be edited from the GUI or from a hand-edited JSON file. */
    public void validate() {
        savingIntervalUnit = savingIntervalUnit == null ? TimeUnit.SECONDS : savingIntervalUnit;
        animationDelayUnit = animationDelayUnit == null ? TimeUnit.SECONDS : animationDelayUnit;

        savingIntervalValue = clamp(savingIntervalValue, 1, savingIntervalUnit.maxValue());
        animationDelayValue = clamp(animationDelayValue, 1, animationDelayUnit.maxValue());
        animationDurationSeconds = clamp(animationDurationSeconds, 1, 15);
        soundVolumePercent = clamp(soundVolumePercent, 0, 100);
        staleRecordMinutes = clamp(staleRecordMinutes, 1, 720);
    }

    public PearlXConfig copy() {
        PearlXConfig copy = new PearlXConfig();
        copy.enabled = enabled;
        copy.trackerEnabled = trackerEnabled;
        copy.animationEnabled = animationEnabled;
        copy.detectionEnabled = detectionEnabled;
        copy.autoSaveEnabled = autoSaveEnabled;
        copy.savingIntervalValue = savingIntervalValue;
        copy.savingIntervalUnit = savingIntervalUnit;
        copy.trackServers = trackServers;
        copy.trackSingleplayer = trackSingleplayer;
        copy.staleRecordMinutes = staleRecordMinutes;
        copy.visualEffectsEnabled = visualEffectsEnabled;
        copy.animationDelayValue = animationDelayValue;
        copy.animationDelayUnit = animationDelayUnit;
        copy.animationDurationSeconds = animationDurationSeconds;
        copy.soundEffectsEnabled = soundEffectsEnabled;
        copy.soundVolumePercent = soundVolumePercent;
        copy.debugEnabled = debugEnabled;
        copy.showTrackingInfo = showTrackingInfo;
        return copy;
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
