package com.alessiodp.libby.configuration;

/**
 * This exception is thrown when an invalid configuration is loaded.
 */
public class ConfigurationException extends RuntimeException {

    /**
     * Creates a new {@code ConfigurationException}.
     *
     * @param message The error message.
     */
    public ConfigurationException(String message) {
        super(message);
    }

    /**
     * Creates a new {@code ConfigurationException}.
     *
     * @param message The error message.
     * @param cause The error that caused this error.
     */
    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new {@code ConfigurationException}.
     *
     * @param cause The error that caused this error.
     */
    public ConfigurationException(Throwable cause) {
        super(cause);
    }
}
