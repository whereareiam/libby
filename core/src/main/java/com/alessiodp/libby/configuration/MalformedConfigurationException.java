package com.alessiodp.libby.configuration;

import org.jetbrains.annotations.NotNull;

/**
 * This exception is thrown when a configuration contains a syntactic error or couldn't be read.
 */
public class MalformedConfigurationException extends ConfigurationException {

    /**
     * Creates a new {@code MalformedConfigurationException}.
     *
     * @param message The error message.
     */
    public MalformedConfigurationException(@NotNull String message) {
        super(message);
    }

    /**
     * Creates a new {@code MalformedConfigurationException}.
     *
     * @param message The error message.
     * @param cause The error that caused this error.
     */
    public MalformedConfigurationException(@NotNull String message, @NotNull Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new {@code MalformedConfigurationException}.
     *
     * @param cause The error that caused this error.
     */
    public MalformedConfigurationException(@NotNull Throwable cause) {
        super(cause);
    }
}
