package com.alessiodp.libby.logging.adapters;

import cn.nukkit.plugin.PluginLogger;
import com.alessiodp.libby.logging.LogLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Logging adapter that logs to a Nukkit plugin logger.
 */
public class NukkitLogAdapter implements LogAdapter {
    /**
     * Nukkit plugin logger
     */
    @NotNull
    private final PluginLogger logger;

    /**
     * Creates a new Nukkit log adapter that logs to a {@link PluginLogger}.
     *
     * @param logger the plugin logger to wrap
     */
    public NukkitLogAdapter(@NotNull PluginLogger logger) {
        this.logger = requireNonNull(logger, "logger");
    }

    /**
     * Logs a message with the provided level to the Nukkit plugin logger.
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
                logger.warning(message);
                break;
            case ERROR:
                logger.error(message);
                break;
        }
    }

    /**
     * Logs a message and stack trace with the provided level to the Nukkit
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
                logger.warning(message, throwable);
                break;
            case ERROR:
                logger.error(message, throwable);
                break;
        }
    }
}
