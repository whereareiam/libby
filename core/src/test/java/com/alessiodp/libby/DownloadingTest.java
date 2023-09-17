package com.alessiodp.libby;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class DownloadingTest {

    private LibraryManagerMock libraryManager;

    @BeforeEach
    void setUp() throws Exception {
        libraryManager = new LibraryManagerMock();
    }

    @AfterEach
    void tearDown() {
        libraryManager = null;
    }

    @Test
    void downloadLibrary() {
        libraryManager.addMavenCentral();
        Path path = libraryManager.downloadLibrary(Library.builder()
                .groupId("org{}apache{}commons")
                .artifactId("commons-lang3")
                .version("3.13.0")
                .build()
        );
        assertTrue(path.toFile().isFile());
    }
}
