package com.alessiodp.libby.configuration;

/**
 * This exception is thrown when a configuration contains a syntactic error or couldn't be read.
 */
public class MalformedConfigurationException extends ConfigurationException {

    /**
     * Creates a new {@code MalformedConfigurationException}.
     *
     * @param message The error message.
     */
    public MalformedConfigurationException(String message) {
        super(message);
    }

    /**
     * Creates a new {@code MalformedConfigurationException}.
     *
     * @param message The error message.
     * @param cause The error that caused this error.
     */
    public MalformedConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new {@code MalformedConfigurationException}.
     *
     * @param cause The error that caused this error.
     */
    public MalformedConfigurationException(Throwable cause) {
        super(cause);
    }
}
