package com.alessiodp.libby;

import com.alessiodp.libby.classloader.IsolatedClassLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.alessiodp.libby.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

public class RelocationTest {

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
    void isolatedRelocatedLoad() throws Exception {
        libraryManager.loadLibrary(APACHE_COMMONS_LANG3_ISOLATED_RELOCATED);

        assertNoneLoaded(libraryManager);

        IsolatedClassLoader isolated = libraryManager.getIsolatedClassLoaderOf(LIBRARY_ID);
        assertNotNull(isolated);
        assertThrows(ClassNotFoundException.class, () -> isolated.loadClass(STRING_UTILS_CLASS));
        assertCorrectlyLoaded(isolated, STRING_UTILS_CLASS_RELOCATED);
    }
}
