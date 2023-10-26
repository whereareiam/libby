package com.alessiodp.libby;

import com.alessiodp.libby.logging.adapters.JDKLogAdapter;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class LibraryManagerMock extends LibraryManager {
    private final List<String> loadedPaths = new ArrayList<>();

    public LibraryManagerMock() throws IOException {
        super(new JDKLogAdapter(Logger.getLogger("LibraryManagerMock")), generateDownloadFolder(), "libs");
    }

    public boolean isLoaded(File file) {
        return loadedPaths.contains(file.getAbsoluteFile().toString());
    }

    public boolean isLoaded(Path file) {
        return isLoaded(file.toFile());
    }

    public List<String> getLoaded() {
        return Collections.unmodifiableList(loadedPaths);
    }

    public Path getSaveDirectory() {
        return super.saveDirectory;
    }

    @Override
    protected void addToClasspath(@NotNull Path file) {
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
