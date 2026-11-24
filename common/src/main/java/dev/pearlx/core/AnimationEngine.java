package dev.pearlx.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Feature Two's engine: the universal Ender Pearl travel animation.
 *
 * <p>It observes thrown pearls itself instead of borrowing the location tracker's state, so
 * disabling the tracker (or its detection or its saving) never disables the animation, and vice
 * versa. It has its own trigger delay (Feature Two's timer), its own duration, and independent
 * visual and sound switches.</p>
 */
public final class AnimationEngine {

    private static final int MAX_SESSIONS = 32;
    private static final int RING_POINTS = 10;

    /** Live pearls seen by this feature, with their own first-seen time. */
    private final Map<String, LivePearl> live = new LinkedHashMap<>();
    private final Map<String, AnimationSession> sessions = new HashMap<>();

    private long lastTriggerAt;
    private int triggerCount;

    public void tick(ClientPort port, PearlXConfig config, List<PearlSnapshot> snapshots, long now) {
        if (!config.animationActive()) {
            if (!sessions.isEmpty() || !live.isEmpty()) {
                sessions.clear();
                live.clear();
                Log.debug("animation feature disabled, running sessions cleared");
            }
            return;
        }

        refreshLivePearls(snapshots, now);
        startEligibleSessions(port, config, now);
        advanceSessions(port, config, now);
    }

    private void refreshLivePearls(List<PearlSnapshot> snapshots, long now) {
        Set<String> seen = new HashSet<>();
        for (PearlSnapshot snapshot : snapshots) {
            if (!snapshot.ownedByLocalPlayer() || snapshot.id() == null) {
                continue;
            }
            seen.add(snapshot.id());
            LivePearl pearl = live.get(snapshot.id());
            if (pearl == null) {
                pearl = new LivePearl(snapshot.id(), now);
                live.put(snapshot.id(), pearl);
            }
            pearl.update(snapshot, now);
        }
        live.keySet().retainAll(seen);
    }

    private void startEligibleSessions(ClientPort port, PearlXConfig config, long now) {
        long triggerDelay = config.animationDelayMillis();
        for (LivePearl pearl : live.values()) {
            if (now - pearl.firstSeenAt < triggerDelay) {
                continue;
            }
            if (sessions.containsKey(pearl.id) || sessions.size() >= MAX_SESSIONS) {
                continue;
            }
            sessions.put(pearl.id, new AnimationSession(pearl.id, now,
                    config.animationDurationMillis(), Math.random() * Math.PI * 2.0));
            lastTriggerAt = now;
            triggerCount++;
            if (config.soundEffectsEnabled) {
                port.playSound(SoundCue.TRAVEL_START, pearl.x, pearl.y, pearl.z,
                        config.soundVolume(), 1.15F);
            }
            Log.debug("animation trigger for " + pearl.id + " after " + triggerDelay + "ms");
        }
    }

    private void advanceSessions(ClientPort port, PearlXConfig config, long now) {
        if (sessions.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<String, AnimationSession>> iterator = sessions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, AnimationSession> entry = iterator.next();
            AnimationSession session = entry.getValue();
            LivePearl pearl = live.get(entry.getKey());

            if (pearl == null) {
                // Landed, retrieved or otherwise gone: cancel and clean up instead of animating air.
                iterator.remove();
                Log.debug("animation cancelled for " + entry.getKey());
                continue;
            }
            if (session.finished(now)) {
                finish(port, config, pearl, session);
                iterator.remove();
                continue;
            }
            if (config.visualEffectsEnabled) {
                renderFrame(port, pearl, session, now);
            }
            session.advance();
        }
    }

    private void renderFrame(ClientPort port, LivePearl pearl, AnimationSession session, long now) {
        double progress = session.progress(now);
        double radius = 0.30 + 1.05 * progress;
        double height = 0.12 + 0.45 * Math.sin(progress * Math.PI);
        int frame = session.frame();

        for (int i = 0; i < RING_POINTS; i++) {
            double angle = session.angleOffset() + (Math.PI * 2.0 * i / RING_POINTS) + progress * 2.2;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            port.spawnParticle(ParticleKind.PORTAL,
                    pearl.x + cos * radius,
                    pearl.y + height + ((i % 2 == 0) ? 0.10 : -0.05),
                    pearl.z + sin * radius,
                    -cos * 0.02, 0.012, -sin * 0.02);
        }

        port.spawnParticle(ParticleKind.END_ROD, pearl.x, pearl.y + 0.35 + height * 0.5, pearl.z,
                0.0, 0.01, 0.0);

        if (progress < 0.35) {
            port.spawnParticle(ParticleKind.REVERSE_PORTAL, pearl.x, pearl.y + 0.2, pearl.z,
                    0.0, 0.0, 0.0);
        }
        if (frame % 3 == 0) {
            port.spawnParticle(ParticleKind.ELECTRIC_SPARK, pearl.x, pearl.y + 0.25, pearl.z,
                    0.02, 0.03, -0.02);
        }
        if (frame % 5 == 0) {
            port.spawnParticle(ParticleKind.DRAGON_BREATH, pearl.x, pearl.y + height, pearl.z,
                    0.01, 0.0, -0.01);
        }
    }

    private void finish(ClientPort port, PearlXConfig config, LivePearl pearl, AnimationSession session) {
        if (config.visualEffectsEnabled) {
            for (int i = 0; i < 8; i++) {
                double angle = session.angleOffset() + Math.PI * 2.0 * i / 8.0;
                port.spawnParticle(ParticleKind.WITCH,
                        pearl.x + Math.cos(angle) * 0.5, pearl.y + 0.3, pearl.z + Math.sin(angle) * 0.5,
                        Math.cos(angle) * 0.05, 0.04, Math.sin(angle) * 0.05);
            }
            for (int i = 0; i < 4; i++) {
                port.spawnParticle(ParticleKind.END_ROD, pearl.x, pearl.y + 0.2 + i * 0.18, pearl.z,
                        0.0, 0.02, 0.0);
            }
        }
        if (config.soundEffectsEnabled) {
            port.playSound(SoundCue.TRAVEL_END, pearl.x, pearl.y, pearl.z,
                    config.soundVolume(), 1.0F);
        }
        Log.debug("animation finished for " + pearl.id);
    }

    /** Stops every running animation, used when leaving a world or when the feature is disabled. */
    public void clear() {
        sessions.clear();
        live.clear();
    }

    public int runningSessions() {
        return sessions.size();
    }

    public int triggerCount() {
        return triggerCount;
    }

    public long lastTriggerAt() {
        return lastTriggerAt;
    }

    public List<String> runningIds() {
        return new ArrayList<>(sessions.keySet());
    }

    /** One pearl observed by the animation feature, with this feature's own timer start. */
    private static final class LivePearl {
        private final String id;
        private final long firstSeenAt;
        private double x;
        private double y;
        private double z;
        private long lastSeenAt;

        private LivePearl(String id, long firstSeenAt) {
            this.id = id;
            this.firstSeenAt = firstSeenAt;
            this.lastSeenAt = firstSeenAt;
        }

        private void update(PearlSnapshot snapshot, long now) {
            x = snapshot.x();
            y = snapshot.y();
            z = snapshot.z();
            lastSeenAt = now;
        }
    }
}
