package com.alessiodp.libby.configuration;

import com.alessiodp.libby.Library;
import com.alessiodp.libby.relocation.Relocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Represents a Libby configuration loaded from a configuration file.
 */
public class Configuration {

    /**
     * The (optional) version of the config.
     */
    private final Optional<Integer> version;

    /**
     * The URL of maven repositories from which libraries will be downloaded.
     */
    private final Set<String> repositories;

    /**
     * The relocations to apply to every library.
     */
    private final List<Relocation> globalRelocations;

    /**
     * The libraries to download and load.
     */
    private final List<Library> libraries;

    /**
     * Creates a new {@code Configuration} instance.
     *
     * @param version the (optional) version of the config
     * @param repositories the URL of maven repositories from which libraries will be downloaded
     * @param globalRelocations the relocations to apply to every library
     * @param libraries the libraries to download and load
     */
    public Configuration(Optional<Integer> version, Set<String> repositories, List<Relocation> globalRelocations, List<Library> libraries) {
        this.version = requireNonNull(version, "version");
        version.ifPresent(ver -> requireNonNull(ver, "version.get()"));
        this.repositories = repositories != null ? Collections.unmodifiableSet(new HashSet<>(repositories)) : Collections.emptySet();
        this.globalRelocations = globalRelocations != null ? Collections.unmodifiableList(new ArrayList<>(globalRelocations)) : Collections.emptyList();
        this.libraries = libraries != null ? Collections.unmodifiableList(new ArrayList<>(libraries)) : Collections.emptyList();
    }

    /**
     * Gets the (optional) version of the config.
     *
     * @return The (optional) version of the config
     */
    public Optional<Integer> getVersion() {
        return version;
    }

    /**
     * Gets the URL of maven repositories from which libraries will be downloaded.
     *
     * @return The URL of maven repositories from which libraries will be downloaded.
     */
    public Set<String> getRepositories() {
        return repositories;
    }

    /**
     * Gets the relocations to apply to every library.
     *
     * @return The relocations to apply to every library.
     */
    public List<Relocation> getGlobalRelocations() {
        return globalRelocations;
    }

    /**
     * Gets the libraries to download and load.
     *
     * @return The libraries to download and load.
     * @see #getGlobalRelocations()
     */
    public List<Library> getLibraries() {
        return libraries;
    }
}
