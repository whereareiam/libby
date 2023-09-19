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
        assertNotLoadedInGlobal();
    }

    @Test
    void loadLibrary() {
        libraryManager.loadLibrary(APACHE_COMMONS_LANG3);

        assertNotLoadedInGlobal();

        // Assert addToClasspath method has been called with the correct file
        List<String> loaded = libraryManager.getLoaded();
        assertEquals(1, loaded.size());
        loaded.forEach(s -> assertCorrectFile(Paths.get(s), APACHE_COMMONS_LANG3.getChecksum()));
    }

    @Test
    void isolatedLoad() throws Exception {
        libraryManager.loadLibrary(APACHE_COMMONS_LANG3_ISOLATED);

        assertNoneLoaded(libraryManager);
        assertNotLoadedInGlobal();

        IsolatedClassLoader isolated = libraryManager.getIsolatedClassLoaderById(LIBRARY_ID);
        assertNotNull(isolated);
        assertCorrectlyLoaded(isolated, STRING_UTILS_CLASS);
    }

    @Test
    void globalIsolatedLoad() throws Exception {
        libraryManager.loadLibrary(APACHE_COMMONS_LANG3_GLOBAL_ISOLATED);

        assertNoneLoaded(libraryManager);

        assertNull(libraryManager.getIsolatedClassLoaderById(LIBRARY_ID));
        assertCorrectlyLoaded(libraryManager.getGlobalIsolatedClassLoader(), STRING_UTILS_CLASS);
    }

    private void assertNotLoadedInGlobal() {
        assertThrows(ClassNotFoundException.class, () -> libraryManager.getGlobalIsolatedClassLoader().loadClass(STRING_UTILS_CLASS));
    }
}
