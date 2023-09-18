package com.alessiodp.libby;

import com.alessiodp.libby.logging.adapters.JDKLogAdapter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class LibraryManagerMock extends LibraryManager {

    private final Set<String> loadedPaths = new HashSet<>();

    public LibraryManagerMock() throws IOException {
        super(new JDKLogAdapter(Logger.getLogger("LibraryManagerMock")), generateDownloadFolder(), "libs");
    }

    public boolean isLoaded(File file) {
        return loadedPaths.contains(file.getAbsoluteFile().toString());
    }

    public boolean isLoaded(Path file) {
        return isLoaded(file.toFile());
    }

    public Set<String> getLoaded() {
        return Collections.unmodifiableSet(loadedPaths);
    }

    @Override
    protected void addToClasspath(Path file) {
        loadedPaths.add(file.toAbsolutePath().toString());
    }

    private static Path generateDownloadFolder() throws IOException {
        Path dir = Paths.get(LibbyTestProperties.BUILD_DIR, "test-downloads");
        assertTrue(dir.isAbsolute(), "test-downloads dir path isn't absolute");
        dir.toFile().mkdirs();
        Path tmpDir = Files.createTempDirectory(dir, null);
        tmpDir.toFile().mkdirs();
        assertTrue(Files.exists(tmpDir), "Couldn't create test-downloads dir");
        return tmpDir;
    }
}
