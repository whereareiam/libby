package com.alessiodp.libby;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Filtered Maven properties and other related constants.
 */
public class LibbyTestProperties {
    /**
     * Build dir path
     */
    public static final String BUILD_DIR = "{{ buildDir }}";
    public static final String TEST_JAR = "{{ testJar }}";

    /**
     * Generates the path for the download folder.
     *
     * @return A path for the download folder
     * @throws IOException If any I/O error happens
     */
    public static Path generateDownloadFolder() throws IOException {
        // Same method as LibraryManagerMock#generateDownloadFolder() in libby:core

        Path dir = Paths.get(LibbyTestProperties.BUILD_DIR, "test-downloads");
        if (!dir.isAbsolute()) {
            throw new IOException("test-downloads dir path isn't absolute");
        }
        dir.toFile().mkdirs();
        Path tmpDir = Files.createTempDirectory(dir, null);
        tmpDir.toFile().mkdirs();
        if (!Files.exists(tmpDir)) {
            throw new IOException("Couldn't create test-downloads dir");
        }
        return tmpDir;
    }
}
