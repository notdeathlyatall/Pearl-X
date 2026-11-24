package dev.pearlx.core;

import java.util.ArrayList;
import java.util.List;

/** Deterministic stand-in for the Minecraft side, so core logic can be tested without a client. */
final class FakePort implements ClientPort {

    long now = 1_700_000_000_000L;
    WorldKey world = WorldKey.server("mc.example.com");
    String dimension = "minecraft:overworld";
    boolean chunksLoaded = true;

    final List<PearlSnapshot> pearls = new ArrayList<>();
    final List<ParticleKind> particles = new ArrayList<>();
    final List<SoundCue> sounds = new ArrayList<>();
    final List<String> messages = new ArrayList<>();

    @Override
    public long nowMillis() {
        return now;
    }

    @Override
    public WorldKey currentWorldKey() {
        return world;
    }

    @Override
    public String currentDimension() {
        return dimension;
    }

    @Override
    public List<PearlSnapshot> scanPearls() {
        return new ArrayList<>(pearls);
    }

    @Override
    public boolean isChunkLoaded(String dimension, double x, double z) {
        if (dimension == null || !dimension.equals(this.dimension)) {
            return false;
        }
        return chunksLoaded;
    }

    @Override
    public void spawnParticle(ParticleKind kind, double x, double y, double z,
                              double dx, double dy, double dz) {
        particles.add(kind);
    }

    @Override
    public void playSound(SoundCue cue, double x, double y, double z, float volume, float pitch) {
        if (volume > 0.0F) {
            sounds.add(cue);
        }
    }

    @Override
    public void info(String message) {
        messages.add(message);
    }

    // ------------------------------------------------------------------ helpers

    void throwPearl(String id, boolean owned, double x, double y, double z) {
        pearls.removeIf(pearl -> pearl.id().equals(id));
        pearls.add(new PearlSnapshot(id, owned, x, y, z, 0.0, 0.0, 0.0, dimension));
    }

    void movePearl(String id, double x, double y, double z) {
        for (int i = 0; i < pearls.size(); i++) {
            PearlSnapshot pearl = pearls.get(i);
            if (pearl.id().equals(id)) {
                pearls.set(i, new PearlSnapshot(pearl.id(), pearl.ownedByLocalPlayer(), x, y, z,
                        pearl.motionX(), pearl.motionY(), pearl.motionZ(), pearl.dimension()));
                return;
            }
        }
    }

    void removePearl(String id) {
        pearls.removeIf(pearl -> pearl.id().equals(id));
    }

    void advanceSeconds(double seconds) {
        now += (long) (seconds * 1000.0);
    }
}
