package com.alessiodp.libby.logging.adapters;

import com.alessiodp.libby.logging.LogLevel;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Logging adapter that logs to a Sponge plugin logger.
 */
public class SpongeLogAdapter implements LogAdapter {
    /**
     * Sponge plugin logger
     */
    @NotNull
    private final Logger logger;

    /**
     * Creates a new Sponge log adapter that logs to a {@link Logger}.
     *
     * @param logger the plugin logger to wrap
     */
    public SpongeLogAdapter(@NotNull Logger logger) {
        this.logger = requireNonNull(logger, "logger");
    }

    /**
     * Logs a message with the provided level to the Sponge plugin logger.
     *
     * @param level   message severity level
     * @param message the message to log
     */
    @Override
    public void log(@NotNull LogLevel level, @Nullable String message) {
        switch (requireNonNull(level, "level")) {
            case DEBUG:
                logger.debug(message);
                break;
            case INFO:
                logger.info(message);
                break;
            case WARN:
                logger.warn(message);
                break;
            case ERROR:
                logger.error(message);
                break;
        }
    }

    /**
     * Logs a message and stack trace with the provided level to the Sponge
     * plugin logger.
     *
     * @param level     message severity level
     * @param message   the message to log
     * @param throwable the throwable to print
     */
    @Override
    public void log(@NotNull LogLevel level, @Nullable String message, @Nullable Throwable throwable) {
        switch (requireNonNull(level, "level")) {
            case DEBUG:
                logger.debug(message, throwable);
                break;
            case INFO:
                logger.info(message, throwable);
                break;
            case WARN:
                logger.warn(message, throwable);
                break;
            case ERROR:
                logger.error(message, throwable);
                break;
        }
    }
}
