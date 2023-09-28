package com.alessiodp.libby;

import cn.nukkit.plugin.Plugin;
import com.alessiodp.libby.logging.adapters.LogAdapter;
import com.alessiodp.libby.logging.adapters.NukkitLogAdapter;
import com.alessiodp.libby.classloader.URLClassLoaderHelper;
import org.jetbrains.annotations.NotNull;

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
    @NotNull
    private final URLClassLoaderHelper classLoader;

    @NotNull
    private final Plugin plugin;

    /**
     * Creates a new Nukkit library manager.
     *
     * @param plugin the plugin to manage
     */
    public NukkitLibraryManager(@NotNull Plugin plugin) {
        this(plugin, "lib");
    }

    /**
     * Creates a new Nukkit library manager.
     *
     * @param plugin the plugin to manage
     * @param directoryName download directory name
     */
    public NukkitLibraryManager(@NotNull Plugin plugin, @NotNull String directoryName) {
        this(plugin, directoryName, new NukkitLogAdapter(requireNonNull(plugin, "plugin").getLogger()));
    }
    
    /**
     * Creates a new Nukkit library manager.
     *
     * @param plugin the plugin to manage
     * @param directoryName download directory name
     * @param logAdapter the log adapter to use
     */
    public NukkitLibraryManager(@NotNull Plugin plugin, @NotNull String directoryName, @NotNull LogAdapter logAdapter) {
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
    protected void addToClasspath(@NotNull Path file) {
        classLoader.addToClasspath(file);
    }

    @Override
    protected InputStream getPluginResourceAsInputStream(@NotNull String path) throws UnsupportedOperationException {
        return plugin.getResource(path);
    }
}
