package dev.pearlx.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Small JSON helpers shared by the configuration and the location store. */
public final class Json {

    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private Json() {
    }

    /**
     * Reads a JSON file, returning {@code null} when the file does not exist or cannot be parsed.
     * A corrupt file is kept next to the original as {@code .corrupt} so nothing is silently lost.
     */
    public static <T> T read(Path file, Class<T> type) {
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            if (text.isBlank()) {
                return null;
            }
            return GSON.fromJson(text, type);
        } catch (Exception e) {
            Log.warn("Could not read " + file.getFileName() + ": " + e.getMessage());
            try {
                Files.move(file, file.resolveSibling(file.getFileName() + ".corrupt"),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
                // Nothing else we can do; the corrupt file simply stays in place.
            }
            return null;
        }
    }

    /** Writes JSON atomically: temporary file first, then a move onto the target. */
    public static void writeAtomic(Path file, String json) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(tmp, json, StandardCharsets.UTF_8);
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
