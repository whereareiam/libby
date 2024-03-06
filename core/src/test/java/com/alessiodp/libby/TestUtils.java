package com.alessiodp.libby;

import com.alessiodp.libby.classloader.IsolatedClassLoader;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;

public class TestUtils {

    public static final String LIBRARY_ID = "commonsLang3";
    public static final String STRING_UTILS_CLASS = "org.apache.commons.lang3.StringUtils";
    public static final String STRING_UTILS_CLASS_RELOCATED = "libs.org.apache.commons.lang3.StringUtils";

    public static final Library APACHE_COMMONS_LANG3 = Library.builder()
            .groupId("org{}apache{}commons")
            .artifactId("commons-lang3")
            .version("3.13.0")
            .checksumFromBase64("gvUoz3GMejwvMPxbx4TjxqChCxdgXa254WyC7eEeYGQ=")
            .build();
    public static final Library APACHE_COMMONS_LANG3_ISOLATED = Library.builder()
            .groupId(APACHE_COMMONS_LANG3.getGroupId())
            .artifactId(APACHE_COMMONS_LANG3.getArtifactId())
            .version(APACHE_COMMONS_LANG3.getVersion())
            .checksum(APACHE_COMMONS_LANG3.getChecksum())
            .isolatedLoad(true)
            .loaderId(LIBRARY_ID)
            .build();
    public static final Library APACHE_COMMONS_LANG3_GLOBAL_ISOLATED = Library.builder()
            .groupId(APACHE_COMMONS_LANG3.getGroupId())
            .artifactId(APACHE_COMMONS_LANG3.getArtifactId())
            .version(APACHE_COMMONS_LANG3.getVersion())
            .checksum(APACHE_COMMONS_LANG3.getChecksum())
            .isolatedLoad(true)
            .build();
    public static final Library APACHE_COMMONS_LANG3_ISOLATED_RELOCATED = Library.builder()
            .groupId(APACHE_COMMONS_LANG3.getGroupId())
            .artifactId(APACHE_COMMONS_LANG3.getArtifactId())
            .version(APACHE_COMMONS_LANG3.getVersion())
            .checksum(APACHE_COMMONS_LANG3.getChecksum())
            .isolatedLoad(true)
            .loaderId(LIBRARY_ID)
            .relocate("org.apache.commons.lang3", "libs.org.apache.commons.lang3")
            .build();

    public static void assertNoneLoaded(LibraryManagerMock libraryManager) {
        assertTrue(libraryManager.getLoaded().isEmpty());
    }

    public static void assertCorrectFile(Path file, byte[] expectedChecksum) {
        try {
            // Asserts the file is the Apache Commons Lang3 jar checking its checksum
            byte[] bytes = Files.readAllBytes(file);
            byte[] sha256 = MessageDigest.getInstance("SHA-256").digest(bytes);
            assertArrayEquals(expectedChecksum, sha256);
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static void assertCorrectlyLoaded(IsolatedClassLoader isolated, String stringUtilsClassName) throws Exception {
        // Try to run a method from StringUtils to ensure the library was loaded correctly
        Class<?> stringUtilsClass = isolated.loadClass(stringUtilsClassName);

        Method capitalize = stringUtilsClass.getMethod("capitalize", String.class);
        assertEquals(String.class, capitalize.getReturnType());
        assertTrue(Modifier.isStatic(capitalize.getModifiers()));

        String capitalized = (String) capitalize.invoke(null, "this is a phrase");
        assertEquals("This is a phrase", capitalized);
    }

    public static void assertNotLoaded() {
        assertThrows(ClassNotFoundException.class, () -> Class.forName(STRING_UTILS_CLASS));
        assertThrows(ClassNotFoundException.class, () -> Class.forName(STRING_UTILS_CLASS_RELOCATED));
    }

}
