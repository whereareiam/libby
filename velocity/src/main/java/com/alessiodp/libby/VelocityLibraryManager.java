package com.alessiodp.libby;

import com.alessiodp.libby.logging.adapters.LogAdapter;
import com.alessiodp.libby.logging.adapters.VelocityLogAdapter;
import com.velocitypowered.api.plugin.PluginManager;
import org.slf4j.Logger;

import java.io.InputStream;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

/**
 * A runtime dependency manager for Velocity plugins.
 */
public class VelocityLibraryManager<T> extends LibraryManager {
    /**
     * Velocity plugin manager used for adding files to the plugin's classpath
     */
    private final PluginManager pluginManager;

    /**
     * The plugin instance required by the plugin manager to add files to the
     * plugin's classpath
     */
    private final T plugin;

    /**
     * Creates a new Velocity library manager.
     *
     * @param logger        the plugin logger
     * @param dataDirectory plugin's data directory
     * @param pluginManager Velocity plugin manager
     * @param plugin        the plugin to manage
     */
    public VelocityLibraryManager(T plugin,
                                  Logger logger,
                                  Path dataDirectory,
                                  PluginManager pluginManager) {
        this(plugin, logger, dataDirectory, pluginManager, "lib");
    }

    /**
     * Creates a new Velocity library manager.
     *
     * @param logger        the plugin logger
     * @param dataDirectory plugin's data directory
     * @param pluginManager Velocity plugin manager
     * @param plugin        the plugin to manage
     * @param directoryName download directory name
     */
    public VelocityLibraryManager(T plugin,
                                  Logger logger,
                                  Path dataDirectory,
                                  PluginManager pluginManager,
                                  String directoryName) {
        this(plugin, new VelocityLogAdapter(logger), dataDirectory, pluginManager, directoryName);
    }

    /**
     * Creates a new Velocity library manager.
     *
     * @param logAdapter    the log adapter to use instead of the plugin logger
     * @param dataDirectory plugin's data directory
     * @param pluginManager Velocity plugin manager
     * @param plugin        the plugin to manage
     * @param directoryName download directory name
     */
    public VelocityLibraryManager(T plugin,
                                  LogAdapter logAdapter,
                                  Path dataDirectory,
                                  PluginManager pluginManager,
                                  String directoryName) {
        super(logAdapter, dataDirectory, directoryName);
        this.pluginManager = requireNonNull(pluginManager, "pluginManager");
        this.plugin = requireNonNull(plugin, "plugin");
    }

    @Override
    protected InputStream getPluginResourceAsInputStream(String path) throws UnsupportedOperationException {
        return getClass().getClassLoader().getResourceAsStream(path);
    }

    /**
     * Adds a file to the Velocity plugin's classpath.
     *
     * @param file the file to add
     */
    @Override
    protected void addToClasspath(Path file) {
        pluginManager.addToClasspath(plugin, file);
    }
}
