package com.alessiodp.libby.logging.adapters;

import com.alessiodp.libby.logging.LogLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Logging interface for adapting platform-specific loggers to our logging API.
 */
public interface LogAdapter {
    /**
     * Logs a message with the provided level.
     *
     * @param level   message severity level
     * @param message the message to log
     */
    void log(@NotNull LogLevel level, @Nullable String message);

    /**
     * Logs a message and stack trace with the provided level.
     *
     * @param level     message severity level
     * @param message   the message to log
     * @param throwable the throwable to print
     */
    void log(@NotNull LogLevel level, @Nullable String message, @Nullable Throwable throwable);
}
