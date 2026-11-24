package dev.pearlx.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Stable identity for a server or a single-player world.
 *
 * <p>Server keys use the address the client connected to, single-player keys use the level name.
 * The same key is used for the on-disk file name, so records survive restarts and are never mixed
 * between two different servers that happen to share a dimension.</p>
 */
public record WorldKey(String type, String id) {

    public static final String SERVER = "server";
    public static final String SINGLEPLAYER = "singleplayer";

    public WorldKey {
        type = SINGLEPLAYER.equalsIgnoreCase(type) ? SINGLEPLAYER : SERVER;
        String cleaned = id == null ? "" : id.trim();
        if (cleaned.isEmpty()) {
            cleaned = "unknown";
        }
        if (cleaned.length() > 160) {
            cleaned = cleaned.substring(0, 160);
        }
        id = cleaned;
    }

    public static WorldKey server(String address) {
        return new WorldKey(SERVER, address);
    }

    public static WorldKey singleplayer(String levelName) {
        return new WorldKey(SINGLEPLAYER, levelName);
    }

    public boolean isServer() {
        return SERVER.equals(type);
    }

    public boolean isSingleplayer() {
        return SINGLEPLAYER.equals(type);
    }

    public String label() {
        return isServer() ? "server " + id : "world " + id;
    }

    public String storageId() {
        return type + ":" + id;
    }

    /** File name for this world: readable prefix plus a short hash so any key maps safely to disk. */
    public String fileName() {
        StringBuilder safe = new StringBuilder();
        for (char c : id.toLowerCase(java.util.Locale.ROOT).toCharArray()) {
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '.' || c == '-' || c == '_') {
                safe.append(c);
            } else if (safe.length() == 0 || safe.charAt(safe.length() - 1) != '-') {
                safe.append('-');
            }
        }
        String prefix = safe.length() > 40 ? safe.substring(0, 40) : safe.toString();
        return prefix + "-" + shortHash(storageId()) + ".json";
    }

    /** Parses a {@code type:id} string as written into stored files. */
    public static WorldKey parse(String storageId) {
        if (storageId == null) {
            return null;
        }
        int separator = storageId.indexOf(':');
        if (separator <= 0) {
            return null;
        }
        String parsedType = storageId.substring(0, separator);
        String parsedId = storageId.substring(separator + 1);
        if (parsedId.isBlank()) {
            return null;
        }
        return new WorldKey(parsedType, parsedId);
    }

    private static String shortHash(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                hex.append(String.format("%02x", digest[i]));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(text.hashCode());
        }
    }
}
