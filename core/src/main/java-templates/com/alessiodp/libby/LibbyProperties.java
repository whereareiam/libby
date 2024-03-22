package com.alessiodp.libby;

/**
 * Filtered Maven properties and other related constants.
 */
public class LibbyProperties {
    /**
     * Project version
     */
    public static final String VERSION = "{{ version }}";

    /**
     * User agent string to use when downloading libraries
     */
    public static final String HTTP_USER_AGENT = "libby/" + VERSION;

    /**
     * Checksum of libby-maven-resolver jar
     */
    public static final String LIBBY_MAVEN_RESOLVER_CHECKSUM = "{{ libbyMavenResolverChecksum }}";
}
