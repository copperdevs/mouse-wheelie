package de.siphalor.mousewheelie;

import org.slf4j.LoggerFactory;

// wrapper class for the built in logger so theres a static place to log
public final class Logger {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MouseWheelie.MOD_ID);

    public static void info(String message) {
        LOGGER.info(message);
    }

    public static void info(String message, Object... args) {
        LOGGER.info(message, args);
    }

    public static void warn(String message) {
        LOGGER.warn(message);
    }

    public static void warn(String message, Object... args) {
        LOGGER.warn(message, args);
    }

    public static void error(String message) {
        LOGGER.error(message);
    }

    public static void error(String message, Object... args) {
        LOGGER.error(message, args);
    }
}
