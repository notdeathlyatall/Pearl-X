package dev.pearlx.core;

/** Immutable view of one Ender Pearl entity at the moment the client observed it. */
public record PearlSnapshot(
        String id,
        boolean ownedByLocalPlayer,
        double x,
        double y,
        double z,
        double motionX,
        double motionY,
        double motionZ,
        String dimension) {
}
