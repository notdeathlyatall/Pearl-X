package dev.pearlx.core;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * Local, per-world JSON storage for tracked pearl locations.
 *
 * <p>One small file per server or single-player world, written atomically. Writes are skipped when
 * the serialized payload is unchanged, and periodic writes run on a single background thread so the
 * render thread never blocks on disk. Nothing is ever uploaded anywhere.</p>
 */
public final class RecordStore implements AutoCloseable {

    private static final int FORMAT_VERSION = 1;

    private final Path directory;
    private final ExecutorService io;
    /** Last payload written (or loaded) per world, used to avoid pointless disk writes. */
    private final Map<String, String> lastPayload = new HashMap<>();

    public RecordStore(Path directory) {
        this(directory, true);
    }

    public RecordStore(Path directory, boolean asynchronous) {
        this.directory = directory;
        this.io = asynchronous
                ? Executors.newSingleThreadExecutor(runnable -> {
                    Thread thread = new Thread(runnable, "PearlX-Storage");
                    thread.setDaemon(true);
                    return thread;
                })
                : null;
    }

    public Path directory() {
        return directory;
    }

    /** Loads the stored records for a world. Returns an empty list when nothing is stored. */
    public List<StoredPearl> load(WorldKey key) {
        Path file = fileFor(key);
        WorldFile worldFile = Json.read(file, WorldFile.class);
        List<StoredPearl> result = new ArrayList<>();
        if (worldFile == null || worldFile.pearls == null) {
            return result;
        }
        for (StoredPearl pearl : worldFile.pearls) {
            if (pearl == null || pearl.id == null || pearl.id.isBlank()) {
                continue;
            }
            result.add(pearl);
        }
        synchronized (lastPayload) {
            lastPayload.put(key.storageId(), payload(result));
        }
        Log.debug("loaded " + result.size() + " record(s) for " + key.label());
        return result;
    }

    /** Writes the records for a world if they changed. Returns true when bytes hit the disk. */
    public boolean saveIfChanged(WorldKey key, List<StoredPearl> pearls) {
        return write(key, pearls);
    }

    /** Schedules a write of a copy of the records on the storage thread. */
    public void saveAsync(WorldKey key, List<StoredPearl> pearls) {
        List<StoredPearl> snapshot = new ArrayList<>(pearls);
        if (io == null) {
            write(key, snapshot);
            return;
        }
        try {
            io.execute(() -> {
                try {
                    write(key, snapshot);
                } catch (RuntimeException e) {
                    Log.error("Could not save pearl records for " + key.label(), e);
                }
            });
        } catch (RuntimeException e) {
            // Executor already shut down (game closing): fall back to a direct write.
            write(key, snapshot);
        }
    }

    /** Deletes the stored records for one world. */
    public void delete(WorldKey key) {
        synchronized (lastPayload) {
            lastPayload.remove(key.storageId());
        }
        try {
            Files.deleteIfExists(fileFor(key));
        } catch (IOException e) {
            Log.error("Could not delete pearl records for " + key.label(), e);
        }
    }

    /** Deletes every stored world file whose key matches the filter. Returns files removed. */
    public int deleteWhere(Predicate<WorldKey> filter) {
        int removed = 0;
        for (WorldKey key : storedWorlds()) {
            if (filter.test(key)) {
                delete(key);
                removed++;
            }
        }
        return removed;
    }

    /** Removes every stored pearl record. */
    public int clearAll() {
        int removed = 0;
        for (WorldKey key : storedWorlds()) {
            delete(key);
            removed++;
        }
        synchronized (lastPayload) {
            lastPayload.clear();
        }
        return removed;
    }

    /** Every world key that currently has a file on disk. */
    public Set<WorldKey> storedWorlds() {
        Set<WorldKey> keys = new HashSet<>();
        if (!Files.isDirectory(directory)) {
            return keys;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.json")) {
            for (Path file : stream) {
                StoredPearl[] ignored = null;
                WorldFile header = Json.read(file, WorldFile.class);
                if (header == null || header.world == null) {
                    continue;
                }
                WorldKey key = WorldKey.parse(header.world);
                if (key != null) {
                    keys.add(key);
                }
            }
        } catch (IOException e) {
            Log.error("Could not list stored worlds", e);
        }
        return keys;
    }

    public int worldFileCount() {
        return storedWorlds().size();
    }

    private Path fileFor(WorldKey key) {
        return directory.resolve(key.fileName());
    }

    private boolean write(WorldKey key, List<StoredPearl> pearls) {
        String payload = payload(pearls);
        synchronized (lastPayload) {
            if (payload.equals(lastPayload.get(key.storageId()))) {
                return false;
            }
            if (pearls.isEmpty()) {
                try {
                    Files.deleteIfExists(fileFor(key));
                } catch (IOException e) {
                    Log.error("Could not remove empty record file for " + key.label(), e);
                }
                lastPayload.put(key.storageId(), payload);
                return true;
            }
            WorldFile worldFile = new WorldFile();
            worldFile.world = key.storageId();
            worldFile.label = key.label();
            worldFile.updated = System.currentTimeMillis();
            worldFile.pearls = new ArrayList<>(pearls);
            try {
                Json.writeAtomic(fileFor(key), Json.GSON.toJson(worldFile));
                lastPayload.put(key.storageId(), payload);
                return true;
            } catch (IOException e) {
                Log.error("Could not save pearl records for " + key.label(), e);
                return false;
            }
        }
    }

    /** Normalized payload: stable ordering, no timestamps, so unchanged data is never rewritten. */
    private static String payload(List<StoredPearl> pearls) {
        List<StoredPearl> sorted = new ArrayList<>(pearls);
        sorted.sort(Comparator.comparing(pearl -> pearl.id == null ? "" : pearl.id));
        return Json.GSON.toJson(sorted);
    }

    /** Waits briefly for queued writes, then shuts the storage thread down. */
    @Override
    public void close() {
        if (io == null) {
            return;
        }
        io.shutdown();
        try {
            if (!io.awaitTermination(2, TimeUnit.SECONDS)) {
                io.shutdownNow();
            }
        } catch (InterruptedException e) {
            io.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /** On-disk document for one world. */
    static final class WorldFile {
        int version = FORMAT_VERSION;
        String world;
        String label;
        long updated;
        List<StoredPearl> pearls = new ArrayList<>();
    }
}
