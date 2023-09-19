package com.alessiodp.libby;

import com.alessiodp.libby.classloader.IsolatedClassLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static com.alessiodp.libby.TestUtils.*;

public class DownloadAndLoadTest {

    private static final String LIBRARY_ID = "commonsLang3";
    private static final String STRING_UTILS_CLASS = "org.apache.commons.lang3.StringUtils";

    public static final Library APACHE_COMMONS_LANG3 = Library.builder()
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
            .loaderId(LIBRARY_ID)
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
        assertCorrectFile(downloadedFilePath, APACHE_COMMONS_LANG3.getChecksum());

        assertNoneLoaded(libraryManager);
    }

    @Test
    void loadLibrary() {
        libraryManager.loadLibrary(APACHE_COMMONS_LANG3);

        // Assert addToClasspath method has been called with the correct file
        List<String> loaded = libraryManager.getLoaded();
        assertEquals(1, loaded.size());
        loaded.forEach(s -> assertCorrectFile(Paths.get(s), APACHE_COMMONS_LANG3.getChecksum()));
    }

    @Test
    void isolatedLoad() throws Exception {
        libraryManager.loadLibrary(APACHE_COMMONS_LANG3_ISOLATED);

        assertNoneLoaded(libraryManager);

        IsolatedClassLoader isolated = libraryManager.getIsolatedClassLoaderById(LIBRARY_ID);
        assertNotNull(isolated);
        assertCorrectlyLoaded(isolated, STRING_UTILS_CLASS);
    }
}
