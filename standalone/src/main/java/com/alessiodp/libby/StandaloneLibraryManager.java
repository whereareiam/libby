package com.alessiodp.libby;

import com.alessiodp.libby.classloader.ClassLoaderHelper;
import com.alessiodp.libby.classloader.SystemClassLoaderHelper;
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
    private final ClassLoaderHelper classLoaderHelper;

    /**
     * Creates a new Standalone library manager using the classloader of the current class.
     *
     * @param logAdapter the log adapter to use
     * @param dataDirectory data directory
     */
    public StandaloneLibraryManager(@NotNull LogAdapter logAdapter, @NotNull Path dataDirectory) {
        this(logAdapter, dataDirectory, "lib");
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
        ClassLoader classLoader = getClass().getClassLoader();
        if (classLoader instanceof URLClassLoader) {
            classLoaderHelper = new URLClassLoaderHelper((URLClassLoader) classLoader, this);
        } else if (classLoader == ClassLoader.getSystemClassLoader()) {
            classLoaderHelper = new SystemClassLoaderHelper(classLoader, this);
        } else {
            throw new RuntimeException("Unsupported class loader: " + classLoader.getClass().getName());
        }
    }

    @Override
    protected void addToClasspath(@NotNull Path file) {
        classLoaderHelper.addToClasspath(file);
    }
}
