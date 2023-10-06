package com.alessiodp.libby;

import com.alessiodp.libby.classloader.URLClassLoaderHelper;
import com.alessiodp.libby.logging.adapters.LogAdapter;
import org.jetbrains.annotations.NotNull;

import java.net.URLClassLoader;
import java.nio.file.Path;

/**
 * A runtime dependency manager for standalone java applications.
 */
public class StandaloneLibraryManager extends LibraryManager {
    /**
     * Standalone classpath helper
     */
    @NotNull
    private final URLClassLoaderHelper classLoader;

    /**
     * Creates a new Standalone library manager using the classloader of the current class.
     *
     * @param logAdapter the log adapter to use
     * @param dataDirectory data directory
     */
    public StandaloneLibraryManager(@NotNull LogAdapter logAdapter, @NotNull Path dataDirectory) {
        super(logAdapter, dataDirectory, "lib");
        classLoader = new URLClassLoaderHelper((URLClassLoader) getClass().getClassLoader(), this);
    }

    /**
     * Creates a new Standalone library manager using the classloader of the current class.
     *
     * @param logAdapter the log adapter to use
     * @param dataDirectory data directory
     * @param directoryName download directory name
     */
    public StandaloneLibraryManager(@NotNull LogAdapter logAdapter, @NotNull Path dataDirectory, @NotNull String directoryName) {
        super(logAdapter, dataDirectory, directoryName);
        classLoader = new URLClassLoaderHelper((URLClassLoader) getClass().getClassLoader(), this);
    }

    @Override
    protected void addToClasspath(@NotNull Path file) {
        classLoader.addToClasspath(file);
    }
}
