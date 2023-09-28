package com.alessiodp.libby.classloader;

import com.alessiodp.libby.SpongeLibraryManager;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

/**
 * A reflection-based wrapper around TransformingClassLoader.DelegatedClassLoader
 * for adding URLs to the classpath.
 */
public class SpongeClassLoaderHelper {
    /**
     * The URLClassLoader helper of libby
     */
    @NotNull
    private final URLClassLoaderHelper classLoader;
    
    /**
     * Creates a new Sponge class loader helper that wraps a {@link URLClassLoaderHelper}.
     * @param pluginClassLoader the class loader of the plugin
     * @param libraryManager    the library manager used to download dependencies
     */
    public SpongeClassLoaderHelper(@NotNull ClassLoader pluginClassLoader, @NotNull SpongeLibraryManager<?> libraryManager) {
        requireNonNull(pluginClassLoader, "pluginClassLoader");
        requireNonNull(libraryManager, "libraryManager");
        
        try {
            Field delegatedClassLoaderField = pluginClassLoader.getClass().getDeclaredField("delegatedClassLoader");
            delegatedClassLoaderField.setAccessible(true);
            
            URLClassLoader spongeClassLoader = (URLClassLoader) delegatedClassLoaderField.get(pluginClassLoader);
            classLoader = new URLClassLoaderHelper(spongeClassLoader, libraryManager);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Adds a path to the class loader's classpath.
     *
     * @param path the path to add
     */
    public void addToClasspath(@NotNull Path path) {
        requireNonNull(classLoader, "classLoader");
        classLoader.addToClasspath(path);
    }
}
