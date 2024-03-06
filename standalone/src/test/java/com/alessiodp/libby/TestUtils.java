package com.alessiodp.libby;

public class TestUtils {

    public static final String STRING_UTILS_CLASS = "org.apache.commons.lang3.StringUtils";

    public static final Library APACHE_COMMONS_LANG3 = Library.builder()
            .groupId("org{}apache{}commons")
            .artifactId("commons-lang3")
            .version("3.13.0")
            .checksumFromBase64("gvUoz3GMejwvMPxbx4TjxqChCxdgXa254WyC7eEeYGQ=")
            .build();
}
