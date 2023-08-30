package net.byteflux.libby;

import static java.util.Objects.requireNonNull;

import net.byteflux.libby.logging.LogLevel;
import net.byteflux.libby.logging.Logger;
import net.byteflux.libby.logging.adapters.LogAdapter;

/**
 * Adapts {@link Logger} into {@link LogAdapter}, useful for creating wrapper {@link LibraryManager}
 */
public class LoggerLogAdapter implements LogAdapter {

    /**
     * Delegate {@link Logger}
     */
    private final Logger logger;

    /**
     * Delegate constructor for adapting {@link} into {@link LogAdapter}
     *
     * @param logger delegate
     */
    public LoggerLogAdapter(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void log(LogLevel level, String message) {
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

    @Override
    public void log(LogLevel level, String message, Throwable throwable) {
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

}
