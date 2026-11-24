package dev.pearlx.core;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Top-level Pearl X runtime.
 *
 * <p>Holds the two features side by side but never couples them:</p>
 * <ul>
 *   <li>{@link PearlTracker} + {@link SaveScheduler} = Feature One (locations, saving timer).</li>
 *   <li>{@link AnimationEngine} = Feature Two (travel animation, its own timer).</li>
 * </ul>
 */
public final class PearlXCore {

    private static final long INFO_INTERVAL_MILLIS = 10_000L;

    private final ClientPort port;
    private final Path baseDirectory;

    private ConfigManager configs;
    private RecordStore store;
    private PearlTracker tracker;
    private AnimationEngine animations;
    private SaveScheduler saveScheduler;

    private long lastInfoAt;
    private int infoCounter;

    private final boolean asynchronousStorage;

    public PearlXCore(ClientPort port, Path baseDirectory) {
        this(port, baseDirectory, true);
    }

    /** @param asynchronousStorage false keeps every write on the calling thread (used by tests). */
    public PearlXCore(ClientPort port, Path baseDirectory, boolean asynchronousStorage) {
        this.port = port;
        this.baseDirectory = baseDirectory;
        this.asynchronousStorage = asynchronousStorage;
    }

    public void init() {
        configs = new ConfigManager(baseDirectory.resolve("pearlx.json"));
        configs.load();
        store = new RecordStore(baseDirectory.resolve("worlds"), asynchronousStorage);
        tracker = new PearlTracker(store);
        animations = new AnimationEngine();
        saveScheduler = new SaveScheduler();
        Log.setDebug(configs.get().debugEnabled);
        Log.info("Pearl X initialised (client-side only, nothing leaves this computer)");
    }

    /** Called once per client tick by the adapter. */
    public void tick() {
        long now = port.nowMillis();
        PearlXConfig config = configs.get();
        Log.setDebug(config.debugEnabled);
        configs.flushIfDirty();

        WorldKey world = config.enabled ? port.currentWorldKey() : null;
        boolean worldChanged = isWorldChanged(world);

        tracker.switchWorld(world, config);
        if (worldChanged) {
            // Never let an animation from a previous server/world survive a switch.
            animations.clear();
        }
        if (world == null) {
            return;
        }

        // One scan per tick feeds both features. They keep separate state and separate timers, and
        // each decides for itself whether it is enabled.
        List<PearlSnapshot> snapshots = port.scanPearlsSafe();
        tracker.tick(port, config, snapshots, now);
        animations.tick(port, config, snapshots, now);

        // Feature One's own periodic saving timer. Nothing here touches the animation timer.
        saveScheduler.reconfigure(config, now);
        if (config.autoSaveEnabled && config.trackingActive() && saveScheduler.poll(now)) {
            tracker.flushAsync(config);
            Log.debug("periodic location save for " + world.label());
        }

        if (config.showTrackingInfo) {
            maybeShowInfo(now, world);
        }
    }

    private WorldKey lastWorld;
    private boolean lastWorldInitialised;

    private boolean isWorldChanged(WorldKey world) {
        if (!lastWorldInitialised || !java.util.Objects.equals(lastWorld, world)) {
            lastWorld = world;
            lastWorldInitialised = true;
            return true;
        }
        return false;
    }

    private void maybeShowInfo(long now, WorldKey world) {
        if (now - lastInfoAt < INFO_INTERVAL_MILLIS) {
            return;
        }
        lastInfoAt = now;
        infoCounter++;
        List<TrackedPearl> pearls = tracker.allPearls();
        int active = tracker.activePearls().size();
        StringBuilder line = new StringBuilder("Pearl X: ");
        line.append(pearls.size()).append(pearls.size() == 1 ? " pearl tracked" : " pearls tracked");
        line.append(" (").append(active).append(" visible) in ").append(world.label());
        if (!pearls.isEmpty()) {
            TrackedPearl nearest = pearls.get(0);
            line.append(" - nearest ").append(formatPosition(nearest));
        }
        line.append(" | animations running: ").append(animations.runningSessions());
        port.info(line.toString());
    }

    private static String formatPosition(TrackedPearl pearl) {
        return String.format(Locale.ROOT, "%.0f %.0f %.0f", pearl.x(), pearl.y(), pearl.z());
    }

    // ------------------------------------------------------------------ accessors

    public PearlXConfig config() {
        return configs.get();
    }

    public ConfigManager configManager() {
        return configs;
    }

    public RecordStore store() {
        return store;
    }

    public PearlTracker tracker() {
        return tracker;
    }

    public AnimationEngine animations() {
        return animations;
    }

    public SaveScheduler saveScheduler() {
        return saveScheduler;
    }

    public WorldKey currentWorld() {
        return tracker.world();
    }

    public int trackedCount() {
        return tracker.size();
    }

    /** Called when the client shuts down: persist everything and stop the storage thread. */
    public void shutdown() {
        try {
            if (tracker != null) {
                tracker.flush(configs.get());
            }
            if (configs != null) {
                configs.saveNow();
            }
        } finally {
            if (store != null) {
                store.close();
            }
        }
    }

    // ------------------------------------------------------- storage management

    public int resetCurrentServer() {
        WorldKey world = tracker.world();
        if (world == null || !world.isServer()) {
            port.info("Pearl X: you are not on a server right now.");
            return 0;
        }
        tracker.forget(world);
        store.delete(world);
        port.info("Pearl X: cleared saved pearl locations for server " + world.id() + ".");
        Log.info("Reset records for " + world.label());
        return 1;
    }

    public int resetCurrentSingleplayerWorld() {
        WorldKey world = tracker.world();
        if (world == null || !world.isSingleplayer()) {
            port.info("Pearl X: you are not in a single-player world right now.");
            return 0;
        }
        tracker.forget(world);
        store.delete(world);
        port.info("Pearl X: cleared saved pearl locations for world " + world.id() + ".");
        Log.info("Reset records for " + world.label());
        return 1;
    }

    public int resetAllServers() {
        int removed = store.deleteWhere(WorldKey::isServer);
        tracker.forget(null);
        port.info("Pearl X: cleared " + removed + " saved server world(s).");
        return removed;
    }

    public int resetAllSingleplayerWorlds() {
        int removed = store.deleteWhere(WorldKey::isSingleplayer);
        tracker.forget(null);
        port.info("Pearl X: cleared " + removed + " saved single-player world(s).");
        return removed;
    }

    public int clearAllRecords() {
        int removed = store.clearAll();
        WorldKey world = tracker.world();
        if (world != null) {
            tracker.forget(world);
        }
        port.info("Pearl X: cleared all saved tracking records (" + removed + " world file(s)).");
        return removed;
    }

    public void resetConfiguration() {
        configs.resetToDefaults();
        Log.setDebug(configs.get().debugEnabled);
        port.info("Pearl X: configuration reset to defaults.");
    }

    public ClientPort port() {
        return port;
    }

    public Path baseDirectory() {
        return baseDirectory;
    }
}
