package dev.pearlx.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything the version-specific adapter must provide. The core never imports Minecraft types,
 * which is what allows one shared implementation to run on every supported game version.
 */
public interface ClientPort {

    long nowMillis();

    /** The current server or single-player world, or {@code null} when no world is loaded. */
    WorldKey currentWorldKey();

    /** Dimension id of the loaded world, or {@code null} when no world is loaded. */
    String currentDimension();

    /** Every Ender Pearl entity currently known to the client, with ownership resolved. */
    List<PearlSnapshot> scanPearls();

    /**
     * Whether the chunk containing the given world coordinates is currently loaded. This is how
     * the tracker distinguishes "the pearl is gone" from "the pearl is merely out of view".
     */
    boolean isChunkLoaded(String dimension, double x, double z);

    void spawnParticle(ParticleKind kind, double x, double y, double z, double dx, double dy, double dz);

    void playSound(SoundCue cue, double x, double y, double z, float volume, float pitch);

    /** Sends a short status line to the player's chat, used by the storage buttons and info mode. */
    void info(String message);

    /** Local player's display name, for the tracking-info line. */
    default String playerName() {
        return "player";
    }

    default List<PearlSnapshot> scanPearlsSafe() {
        try {
            List<PearlSnapshot> snapshots = scanPearls();
            return snapshots == null ? List.of() : snapshots;
        } catch (RuntimeException e) {
            Log.warn("Pearl scan failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}
