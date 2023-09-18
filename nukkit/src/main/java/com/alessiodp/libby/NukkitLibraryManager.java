package com.alessiodp.libby;

import cn.nukkit.plugin.Plugin;
import com.alessiodp.libby.logging.adapters.LogAdapter;
import com.alessiodp.libby.logging.adapters.NukkitLogAdapter;
import com.alessiodp.libby.classloader.URLClassLoaderHelper;

import java.io.InputStream;
import java.net.URLClassLoader;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

/**
 * A runtime dependency manager for Nukkit plugins.
 */
public class NukkitLibraryManager extends LibraryManager {
    /**
     * Plugin classpath helper
     */
    private final URLClassLoaderHelper classLoader;

    private final Plugin plugin;

    /**
     * Creates a new Nukkit library manager.
     *
     * @param plugin the plugin to manage
     */
    public NukkitLibraryManager(Plugin plugin) {
        this(plugin, "lib");
    }

    /**
     * Creates a new Nukkit library manager.
     *
     * @param plugin the plugin to manage
     * @param directoryName download directory name
     */
    public NukkitLibraryManager(Plugin plugin, String directoryName) {
        this(plugin, directoryName, new NukkitLogAdapter(requireNonNull(plugin, "plugin").getLogger()));
    }
    
    /**
     * Creates a new Nukkit library manager.
     *
     * @param plugin the plugin to manage
     * @param directoryName download directory name
     * @param logAdapter the log adapter to use
     */
    public NukkitLibraryManager(Plugin plugin, String directoryName, LogAdapter logAdapter) {
        super(logAdapter, plugin.getDataFolder().toPath(), directoryName);
        classLoader = new URLClassLoaderHelper((URLClassLoader) plugin.getClass().getClassLoader(), this);
        this.plugin = plugin;
    }

    /**
     * Adds a file to the Nukkit plugin's classpath.
     *
     * @param file the file to add
     */
    @Override
    protected void addToClasspath(Path file) {
        classLoader.addToClasspath(file);
    }

    @Override
    protected InputStream getPluginResourceAsInputStream(String path) throws UnsupportedOperationException {
        return plugin.getResource(path);
    }
}
