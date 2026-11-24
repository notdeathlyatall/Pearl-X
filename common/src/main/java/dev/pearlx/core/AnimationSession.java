package dev.pearlx.core;

/** One running travel animation. Each pearl owns at most one session, which prevents duplicates. */
final class AnimationSession {

    private final String pearlId;
    private final long startedAt;
    private final long durationMillis;
    private final double angleOffset;
    private long lastFrameAt;
    private int frame;

    AnimationSession(String pearlId, long startedAt, long durationMillis, double angleOffset) {
        this.pearlId = pearlId;
        this.startedAt = startedAt;
        this.durationMillis = Math.max(1L, durationMillis);
        this.angleOffset = angleOffset;
        this.lastFrameAt = startedAt;
    }

    String pearlId() {
        return pearlId;
    }

    long startedAt() {
        return startedAt;
    }

    long durationMillis() {
        return durationMillis;
    }

    double angleOffset() {
        return angleOffset;
    }

    int frame() {
        return frame;
    }

    void advance() {
        frame++;
        lastFrameAt = System.currentTimeMillis();
    }

    long lastFrameAt() {
        return lastFrameAt;
    }

    double progress(long now) {
        return Math.min(1.0, (now - startedAt) / (double) durationMillis);
    }

    boolean finished(long now) {
        return now - startedAt >= durationMillis;
    }
}
