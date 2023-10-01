package com.alessiodp.libby;

import com.alessiodp.libby.classloader.URLClassLoaderHelper;
import com.alessiodp.libby.logging.adapters.JDKLogAdapter;
import com.alessiodp.libby.logging.adapters.LogAdapter;
import net.md_5.bungee.api.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.net.URLClassLoader;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

/**
 * A runtime dependency manager for Bungee plugins.
 */
public class BungeeLibraryManager extends LibraryManager {
    /**
     * Plugin classpath helper
     */
    @NotNull
    private final URLClassLoaderHelper classLoader;

    @NotNull
    private final Plugin plugin;

    /**
     * Creates a new Bungee library manager.
     *
     * @param plugin the plugin to manage
     */
    public BungeeLibraryManager(@NotNull Plugin plugin) {
        this(plugin, "lib");
    }

    /**
     * Creates a new Bungee library manager.
     *
     * @param plugin the plugin to manage
     * @param directoryName download directory name
     */
    public BungeeLibraryManager(@NotNull Plugin plugin, @NotNull String directoryName) {
        this(plugin, directoryName, new JDKLogAdapter(requireNonNull(plugin, "plugin").getLogger()));
    }

    /**
     * Creates a new Bungee library manager.
     *
     * @param plugin the plugin to manage
     * @param directoryName download directory name
     * @param logAdapter the log adapter to use
     */
    public BungeeLibraryManager(@NotNull Plugin plugin, @NotNull String directoryName, @NotNull LogAdapter logAdapter) {
        super(logAdapter, plugin.getDataFolder().toPath(), directoryName);
        classLoader = new URLClassLoaderHelper((URLClassLoader) plugin.getClass().getClassLoader(), this);
        this.plugin = plugin;
    }

    /**
     * Adds a file to the Bungee plugin's classpath.
     *
     * @param file the file to add
     */
    @Override
    protected void addToClasspath(@NotNull Path file) {
        classLoader.addToClasspath(file);
    }

    @Override
    protected InputStream getResourceAsStream(@NotNull String path) {
        return plugin.getResourceAsStream(path);
    }
}
