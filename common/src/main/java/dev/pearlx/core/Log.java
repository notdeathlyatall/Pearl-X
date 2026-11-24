package dev.pearlx.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Logging facade. Debug logging is opt-in from the Advanced configuration tab. */
public final class Log {

    private static final Logger LOGGER = LoggerFactory.getLogger("Pearl X");
    private static volatile boolean debug = false;

    private Log() {
    }

    public static void setDebug(boolean enabled) {
        debug = enabled;
    }

    public static boolean isDebug() {
        return debug;
    }

    public static void info(String message) {
        LOGGER.info(message);
    }

    public static void warn(String message) {
        LOGGER.warn(message);
    }

    public static void error(String message, Throwable throwable) {
        LOGGER.error(message, throwable);
    }

    public static void debug(String message) {
        if (debug) {
            LOGGER.info("[debug] {}", message);
        }
    }
}
