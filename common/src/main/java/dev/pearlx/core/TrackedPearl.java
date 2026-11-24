package dev.pearlx.core;

/**
 * One pearl the tracker knows about.
 *
 * <p>{@code sessionPearl} is true only for pearls observed being thrown during this session. Pearls
 * restored from disk keep it false, which is what stops an outdated animation from firing after a
 * restart or a server switch.</p>
 */
public final class TrackedPearl {

    private static final double MEANINGFUL_MOVE = 0.25;

    private final String id;
    private final long thrownAt;
    private final boolean sessionPearl;

    private double x;
    private double y;
    private double z;
    private double motionX;
    private double motionY;
    private double motionZ;
    private String dimension;
    private PearlStatus status;
    private long lastSeenAt;

    private TrackedPearl(String id, long thrownAt, boolean sessionPearl, double x, double y, double z,
                         String dimension, PearlStatus status, long lastSeenAt) {
        this.id = id;
        this.thrownAt = thrownAt;
        this.sessionPearl = sessionPearl;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
        this.status = status;
        this.lastSeenAt = lastSeenAt;
    }

    public static TrackedPearl observed(PearlSnapshot snapshot, long now) {
        return new TrackedPearl(snapshot.id(), now, true, snapshot.x(), snapshot.y(), snapshot.z(),
                snapshot.dimension(), PearlStatus.ACTIVE, now);
    }

    public static TrackedPearl restored(StoredPearl stored) {
        long thrownAt = stored.thrownAt > 0 ? stored.thrownAt : System.currentTimeMillis();
        return new TrackedPearl(stored.id, thrownAt, false, stored.x, stored.y, stored.z,
                stored.dimension == null ? "minecraft:overworld" : stored.dimension,
                PearlStatus.RESTORED, 0L);
    }

    public String id() {
        return id;
    }

    public long thrownAt() {
        return thrownAt;
    }

    public boolean sessionPearl() {
        return sessionPearl;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public String dimension() {
        return dimension;
    }

    public PearlStatus status() {
        return status;
    }

    public long lastSeenAt() {
        return lastSeenAt;
    }

    /** Updates the position from a live observation. Returns true when the record meaningfully changed. */
    boolean update(PearlSnapshot snapshot, long now) {
        double dx = snapshot.x() - x;
        double dy = snapshot.y() - y;
        double dz = snapshot.z() - z;
        boolean moved = (dx * dx + dy * dy + dz * dz) > MEANINGFUL_MOVE * MEANINGFUL_MOVE;
        boolean dimensionChanged = !snapshot.dimension().equals(dimension);
        boolean wasInactive = status != PearlStatus.ACTIVE;

        x = snapshot.x();
        y = snapshot.y();
        z = snapshot.z();
        motionX = snapshot.motionX();
        motionY = snapshot.motionY();
        motionZ = snapshot.motionZ();
        dimension = snapshot.dimension();
        status = PearlStatus.ACTIVE;
        lastSeenAt = now;
        return moved || dimensionChanged || wasInactive;
    }

    void markUnloaded(long now) {
        status = PearlStatus.UNLOADED;
        lastSeenAt = now;
    }

    public StoredPearl toStored() {
        return new StoredPearl(id, round(x), round(y), round(z), dimension, thrownAt,
                System.currentTimeMillis(), status.name());
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    @Override
    public String toString() {
        return id + "@" + String.format(java.util.Locale.ROOT, "%.1f,%.1f,%.1f", x, y, z)
                + " [" + status + ", " + dimension + "]";
    }
}
