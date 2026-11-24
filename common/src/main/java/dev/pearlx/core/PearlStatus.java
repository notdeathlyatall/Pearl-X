package dev.pearlx.core;

/** Lifecycle state of a tracked pearl record. */
public enum PearlStatus {
    /** The client currently observes the pearl entity. */
    ACTIVE,
    /** The pearl entity is not visible right now; its chunk may simply be unloaded. */
    UNLOADED,
    /** Loaded from disk after a restart, waiting to be re-associated with a live entity. */
    RESTORED
}
