package dev.pearlx.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Feature One's tracking engine.
 *
 * <p>Owns the in-memory set of tracked pearls for the current world and decides when a record is
 * finished. It never reads animation settings and never triggers effects.</p>
 */
public final class PearlTracker {

    private static final int MAX_TRACKED = 128;

    private final RecordStore store;
    private final Map<String, TrackedPearl> pearls = new LinkedHashMap<>();

    private WorldKey world;
    private boolean needsRestore;

    public PearlTracker(RecordStore store) {
        this.store = store;
    }

    public WorldKey world() {
        return world;
    }

    /**
     * Switches to another server/world. Records for the world being left are persisted first so a
     * disconnect, a server switch or a crash never loses the last known locations.
     */
    public void switchWorld(WorldKey newWorld, PearlXConfig config) {
        if (Objects.equals(world, newWorld)) {
            return;
        }
        if (world != null) {
            flush(config);
            Log.debug("left " + world.label() + ", stored records flushed");
        }
        pearls.clear();
        world = newWorld;
        needsRestore = false;
        if (newWorld != null) {
            restore();
        }
    }

    private void restore() {
        for (StoredPearl stored : store.load(world)) {
            TrackedPearl pearl = TrackedPearl.restored(stored);
            pearls.put(pearl.id(), pearl);
        }
        if (!pearls.isEmpty()) {
            Log.info("Restored " + pearls.size() + " saved pearl record(s) for " + world.label());
        }
    }

    /** One tracking pass over the pearls the client scanned this tick. */
    public void tick(ClientPort port, PearlXConfig config, List<PearlSnapshot> snapshots, long now) {
        if (world == null) {
            return;
        }
        boolean active = config.trackingActive() && config.trackingAllowedFor(world.isServer());
        if (!active) {
            if (!pearls.isEmpty()) {
                flush(config);
                pearls.clear();
                Log.debug("tracking disabled, active records flushed and cleared from memory");
            }
            needsRestore = true;
            return;
        }
        if (needsRestore) {
            needsRestore = false;
            restore();
        }

        Set<String> seen = new HashSet<>();

        for (PearlSnapshot snapshot : snapshots) {
            if (!snapshot.ownedByLocalPlayer() || snapshot.id() == null) {
                continue;
            }
            seen.add(snapshot.id());
            TrackedPearl pearl = pearls.get(snapshot.id());
            if (pearl == null) {
                if (pearls.size() >= MAX_TRACKED) {
                    pruneOne();
                    if (pearls.size() >= MAX_TRACKED) {
                        continue;
                    }
                }
                pearl = TrackedPearl.observed(snapshot, now);
                pearls.put(pearl.id(), pearl);
                Log.debug("started tracking " + pearl);
            } else {
                if (pearl.update(snapshot, now)) {
                    Log.debug("updated " + pearl);
                }
            }
        }

        Iterator<Map.Entry<String, TrackedPearl>> iterator = pearls.entrySet().iterator();
        while (iterator.hasNext()) {
            TrackedPearl pearl = iterator.next().getValue();
            if (seen.contains(pearl.id())) {
                continue;
            }
            if (pearl.status() == PearlStatus.ACTIVE) {
                pearl.markUnloaded(now);
            }
            // A pearl whose chunk is loaded but whose entity is gone has landed, been retrieved or
            // despawned. When the chunk is not loaded the entity is simply not synchronised yet, so
            // the record is kept until it either reappears or expires.
            boolean chunkLoaded = port.isChunkLoaded(pearl.dimension(), pearl.x(), pearl.z());
            if (chunkLoaded) {
                iterator.remove();
                Log.debug("dropped " + pearl.id() + " (landed or retrieved)");
            } else if (now - pearl.lastSeenAt() > config.staleRecordMillis()) {
                iterator.remove();
                Log.debug("dropped " + pearl.id() + " (unloaded chunk expired)");
            }
        }
    }

    private void pruneOne() {
        TrackedPearl oldest = null;
        for (TrackedPearl pearl : pearls.values()) {
            if (pearl.status() != PearlStatus.ACTIVE
                    && (oldest == null || pearl.lastSeenAt() < oldest.lastSeenAt())) {
                oldest = pearl;
            }
        }
        if (oldest != null) {
            pearls.remove(oldest.id());
        }
    }

    /** Persists the current world's records immediately if anything changed. */
    public boolean flush(PearlXConfig config) {
        if (world == null) {
            return false;
        }
        return store.saveIfChanged(world, snapshotRecords());
    }

    /** Queues a save on the storage thread, used by Feature One's periodic save timer. */
    public void flushAsync(PearlXConfig config) {
        if (world == null) {
            return;
        }
        store.saveAsync(world, snapshotRecords());
    }

    private List<StoredPearl> snapshotRecords() {
        List<StoredPearl> records = new ArrayList<>(pearls.size());
        for (TrackedPearl pearl : pearls.values()) {
            records.add(pearl.toStored());
        }
        return records;
    }

    public List<TrackedPearl> activePearls() {
        List<TrackedPearl> active = new ArrayList<>();
        for (TrackedPearl pearl : pearls.values()) {
            if (pearl.status() == PearlStatus.ACTIVE) {
                active.add(pearl);
            }
        }
        return active;
    }

    public List<TrackedPearl> allPearls() {
        return new ArrayList<>(pearls.values());
    }

    public int size() {
        return pearls.size();
    }

    /** Drops in-memory records for a world after its stored data was deleted. */
    public void forget(WorldKey key) {
        if (key == null || Objects.equals(world, key)) {
            pearls.clear();
            needsRestore = true;
        }
    }
}
