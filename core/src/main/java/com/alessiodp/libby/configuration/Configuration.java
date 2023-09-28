package com.alessiodp.libby.configuration;

import com.alessiodp.libby.Library;
import com.alessiodp.libby.relocation.Relocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a Libby configuration loaded from a configuration file.
 */
public class Configuration {

    /**
     * The (optional) version of the config.
     */
    @Nullable
    private final Integer version;

    /**
     * The URL of maven repositories from which libraries will be downloaded.
     */
    @NotNull
    private final Set<String> repositories;

    /**
     * The relocations to apply to every library.
     */
    @NotNull
    private final Set<Relocation> globalRelocations;

    /**
     * The libraries to download and load.
     */
    @NotNull
    private final List<Library> libraries;

    /**
     * Creates a new {@code Configuration} instance.
     *
     * @param version the (optional) version of the config
     * @param repositories the URL of maven repositories from which libraries will be downloaded
     * @param globalRelocations the relocations to apply to every library
     * @param libraries the libraries to download and load
     */
    public Configuration(@Nullable Integer version, @Nullable Set<String> repositories, @Nullable Set<Relocation> globalRelocations, @Nullable List<Library> libraries) {
        this.version = version;
        this.repositories = repositories != null ? Collections.unmodifiableSet(new HashSet<>(repositories)) : Collections.emptySet();
        this.globalRelocations = globalRelocations != null ? Collections.unmodifiableSet(new HashSet<>(globalRelocations)) : Collections.emptySet();
        this.libraries = libraries != null ? Collections.unmodifiableList(new ArrayList<>(libraries)) : Collections.emptyList();
    }

    /**
     * Gets the (optional) version of the config.
     *
     * @return The (optional) version of the config
     */
    @Nullable
    public Integer getVersion() {
        return version;
    }

    /**
     * Gets the URL of maven repositories from which libraries will be downloaded.
     *
     * @return The URL of maven repositories from which libraries will be downloaded.
     */
    @NotNull
    public Set<String> getRepositories() {
        return repositories;
    }

    /**
     * Gets the relocations to apply to every library.
     *
     * @return The relocations to apply to every library.
     */
    @NotNull
    public Set<Relocation> getGlobalRelocations() {
        return globalRelocations;
    }

    /**
     * Gets the libraries to download and load.
     *
     * @return The libraries to download and load.
     * @see #getGlobalRelocations()
     */
    @NotNull
    public List<Library> getLibraries() {
        return libraries;
    }
}
