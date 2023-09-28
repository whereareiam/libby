package com.alessiodp.libby;

import com.alessiodp.libby.classloader.SpongeClassLoaderHelper;
import com.alessiodp.libby.logging.adapters.LogAdapter;
import com.alessiodp.libby.logging.adapters.SpongeLogAdapter;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

/**
 * A runtime dependency manager for Sponge plugins.
 */
public class SpongeLibraryManager<T> extends LibraryManager {
    /**
     * Plugin classpath helper
     */
    private final SpongeClassLoaderHelper classLoader;

    /**
     * Creates a new Sponge library manager.
     *
     * @param plugin        the plugin to manage
     * @param logger        the plugin logger
     * @param dataDirectory plugin's data directory
     */
    public SpongeLibraryManager(T plugin, Logger logger, Path dataDirectory) {
        this(plugin, logger, dataDirectory, "lib");
    }

    /**
     * Creates a new Sponge library manager.
     *
     * @param plugin        the plugin to manage
     * @param logger        the plugin logger
     * @param dataDirectory plugin's data directory
     * @param directoryName download directory name
     */
    public SpongeLibraryManager(T plugin, Logger logger, Path dataDirectory, String directoryName) {
        this(plugin, new SpongeLogAdapter(logger), dataDirectory, directoryName);
    }

    /**
     * Creates a new Sponge library manager.
     *
     * @param plugin        the plugin to manage
     * @param logAdapter    the log adapter to use instead of the plugin logger
     * @param dataDirectory plugin's data directory
     * @param directoryName download directory name
     */
    public SpongeLibraryManager(T plugin, LogAdapter logAdapter, Path dataDirectory, String directoryName) {
        super(logAdapter, dataDirectory, directoryName);
        classLoader = new SpongeClassLoaderHelper((plugin.getClass().getClassLoader()), this);
    }

    @Override
    protected InputStream getPluginResourceAsInputStream(String path) throws UnsupportedOperationException {
        return getClass().getClassLoader().getResourceAsStream(path);
    }

    /**
     * Adds a file to the Sponge plugin's classpath.
     *
     * @param file the file to add
     */
    @Override
    protected void addToClasspath(Path file) {
        requireNonNull(classLoader, "classLoader");
        classLoader.addToClasspath(file);
    }
}
