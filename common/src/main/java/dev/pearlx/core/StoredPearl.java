package dev.pearlx.core;

/** Persisted shape of one pearl location record inside a per-world JSON file. */
public final class StoredPearl {

    public String id;
    public double x;
    public double y;
    public double z;
    public String dimension;
    public long thrownAt;
    public long updatedAt;
    public String status;

    public StoredPearl() {
        // Gson
    }

    public StoredPearl(String id, double x, double y, double z, String dimension,
                       long thrownAt, long updatedAt, String status) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
        this.thrownAt = thrownAt;
        this.updatedAt = updatedAt;
        this.status = status;
    }
}
