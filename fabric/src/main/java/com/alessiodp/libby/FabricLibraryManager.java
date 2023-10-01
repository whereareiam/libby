package com.alessiodp.libby;

import com.alessiodp.libby.logging.adapters.FabricLogAdapter;
import com.alessiodp.libby.logging.adapters.LogAdapter;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

/**
 * A runtime dependency manager for Fabric mods.
 */
public class FabricLibraryManager extends LibraryManager {
    /**
     * Mod container of Fabric
     */
    private final ModContainer modContainer;
    
    /**
     * Creates a new Fabric library manager.
     *
     * @param modId the id of the mod
     * @param logger the mod logger
     */
    public FabricLibraryManager(@NotNull String modId, @NotNull Logger logger) {
        this(modId, logger, "lib");
    }
    
    /**
     * Creates a new Fabric library manager.
     *
     * @param modId         the id of the mod
     * @param logger        the mod logger
     * @param directoryName download directory name
     */
    public FabricLibraryManager(@NotNull String modId, @NotNull Logger logger, @NotNull String directoryName) {
        this(modId, new FabricLogAdapter(logger), directoryName);
    }
    
    /**
     * Creates a new Fabric library manager.
     *
     * @param modId         the id of the mod
     * @param logAdapter    the log adapter to use instead of the mod logger
     * @param directoryName download directory name
     */
    public FabricLibraryManager(@NotNull String modId, @NotNull LogAdapter logAdapter, @NotNull String directoryName) {
        super(logAdapter, FabricLoader.getInstance().getConfigDir().resolve(modId), directoryName);
        modContainer = FabricLoader.getInstance().getModContainer(requireNonNull(modId, modId)).orElseThrow(() -> new NullPointerException("modContainer"));
    }
    
    /**
     * Adds a file to the Fabric classpath.
     *
     * @param file the file to add
     */
    @Override
    protected void addToClasspath(@NotNull Path file) {
        FabricLauncherBase.getLauncher().addToClassPath(file);
    }
    
    @Override
    protected InputStream getResourceAsStream(@NotNull String path) {
        try {
            return Files.newInputStream(requireNonNull(modContainer.findPath(path).orElse(null)));
        } catch (IOException e) {
            return null;
        }
    }
}
