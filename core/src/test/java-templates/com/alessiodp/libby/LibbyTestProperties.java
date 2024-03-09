package com.alessiodp.libby;

/**
 * Filtered Maven properties and other related constants.
 */
public class LibbyTestProperties {
    /**
     * Build dir path
     */
    public static final String BUILD_DIR = "{{ buildDir }}";

    /**
     * Local repo set up by gradle for libby-maven-resolver
     */
    public static final String LIBBY_MAVEN_RESOLVER_REPO = "{{ libbyMavenResolverRepo }}";
}
