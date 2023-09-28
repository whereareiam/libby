package com.alessiodp.libby.configuration;

import org.jetbrains.annotations.NotNull;

/**
 * This exception is thrown when an invalid configuration is loaded.
 */
public class ConfigurationException extends RuntimeException {

    /**
     * Creates a new {@code ConfigurationException}.
     *
     * @param message The error message.
     */
    public ConfigurationException(@NotNull String message) {
        super(message);
    }

    /**
     * Creates a new {@code ConfigurationException}.
     *
     * @param message The error message.
     * @param cause The error that caused this error.
     */
    public ConfigurationException(@NotNull String message, @NotNull Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new {@code ConfigurationException}.
     *
     * @param cause The error that caused this error.
     */
    public ConfigurationException(@NotNull Throwable cause) {
        super(cause);
    }
}
