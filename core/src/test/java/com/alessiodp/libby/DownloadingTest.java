package com.alessiodp.libby;

import com.alessiodp.libby.classloader.IsolatedClassLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class DownloadingTest {

    private static final String LIBRARY_ID = "commonsLang3";
    private static final String STRING_UTILS_CLASS = "org.apache.commons.lang3.StringUtils";

    private static final Library APACHE_COMMONS_LANG3 = Library.builder()
            .groupId("org{}apache{}commons")
            .artifactId("commons-lang3")
            .version("3.13.0")
            .checksum("gvUoz3GMejwvMPxbx4TjxqChCxdgXa254WyC7eEeYGQ=")
            .build();
    private static final Library APACHE_COMMONS_LANG3_ISOLATED = Library.builder()
            .groupId(APACHE_COMMONS_LANG3.getGroupId())
            .artifactId(APACHE_COMMONS_LANG3.getArtifactId())
            .version(APACHE_COMMONS_LANG3.getVersion())
            .checksum(APACHE_COMMONS_LANG3.getChecksum())
            .isolatedLoad(true)
            .id(LIBRARY_ID)
            .build();

    private LibraryManagerMock libraryManager;

    @BeforeEach
    void setUp() throws Exception {
        libraryManager = new LibraryManagerMock();
        libraryManager.addMavenCentral();
        assertNotLoaded();
    }

    @AfterEach
    void tearDown() {
        libraryManager = null;
    }

    @Test
    void downloadLibrary() {
        Path downloadedFilePath = libraryManager.downloadLibrary(APACHE_COMMONS_LANG3);
        assertTrue(downloadedFilePath.toFile().isFile());

        // Make sure downloaded file has the correct checksum
        assertCorrectFile(downloadedFilePath);

        assertNoneLoaded();
    }

    @Test
    void loadLibrary() {
        libraryManager.loadLibrary(APACHE_COMMONS_LANG3);

        // Assert addToClasspath method has been called with the correct file
        Set<String> loaded = libraryManager.getLoaded();
        assertEquals(1, loaded.size());
        loaded.forEach(s -> assertCorrectFile(Paths.get(s)));
    }

    @Test
    void isolatedLoad() throws Exception {
        libraryManager.loadLibrary(APACHE_COMMONS_LANG3_ISOLATED);

        assertNoneLoaded();

        IsolatedClassLoader isolated = libraryManager.getIsolatedClassLoaderOf(LIBRARY_ID);
        assertNotNull(isolated);
        assertCorrectlyLoaded(isolated);
    }

    private void assertNotLoaded() {
        assertThrows(ClassNotFoundException.class, () -> Class.forName(STRING_UTILS_CLASS));
    }

    private void assertNoneLoaded() {
        assertTrue(libraryManager.getLoaded().isEmpty());
    }

    private void assertCorrectFile(Path file) {
        try {
            // Asserts the file is the Apache Commons Lang3 jar checking its checksum
            byte[] bytes = Files.readAllBytes(file);
            byte[] sha256 = MessageDigest.getInstance("SHA-256").digest(bytes);
            assertArrayEquals(APACHE_COMMONS_LANG3.getChecksum(), sha256);
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private void assertCorrectlyLoaded(IsolatedClassLoader isolated) throws Exception {
        // Try to run a method from StringUtils to ensure the library was loaded correctly
        Class<?> stringUtilsClass = isolated.loadClass(STRING_UTILS_CLASS);

        Method capitalize = stringUtilsClass.getMethod("capitalize", String.class);
        assertEquals(String.class, capitalize.getReturnType());
        assertTrue(Modifier.isStatic(capitalize.getModifiers()));

        String capitalized = (String) capitalize.invoke(null, "this is a phrase");
        assertEquals("This is a phrase", capitalized);
    }
}
