package com.alessiodp.libby;

import com.alessiodp.libby.classloader.IsolatedClassLoader;
import com.alessiodp.libby.configuration.Configuration;
import com.alessiodp.libby.configuration.ConfigurationException;
import com.alessiodp.libby.configuration.ConfigurationFetcher;
import com.alessiodp.libby.configuration.MalformedConfigurationException;
import com.alessiodp.libby.logging.LogLevel;
import com.alessiodp.libby.logging.Logger;
import com.alessiodp.libby.relocation.Relocation;
import com.alessiodp.libby.relocation.RelocationHelper;
import com.alessiodp.libby.transitive.TransitiveDependencyHelper;
import com.alessiodp.libby.logging.adapters.LogAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * A runtime dependency manager for java applications.
 * <p>
 * The library manager can resolve a dependency jar through the configured
 * Maven repositories, download it into a local cache, relocate it and then
 * load it into the classloader classpath.
 * <p>
 * Transitive dependencies for a library aren't downloaded automatically and
 * must be explicitly loaded like every other library.
 * <p>
 * It's recommended that libraries are relocated to prevent any namespace
 * conflicts with different versions of the same library bundled with other
 * java applications or maybe even bundled with the server itself.
 *
 * @see Library
 */
public abstract class LibraryManager {
    /**
     * Wrapped logger
     */
    protected final Logger logger;

    /**
     * Directory where downloaded library jars are saved to
     */
    protected final Path saveDirectory;

    /**
     * Maven repositories used to resolve artifacts
     */
    private final Set<String> repositories = new LinkedHashSet<>();

    /**
     * Lazily-initialized relocation helper that uses reflection to call into
     * Luck's Jar Relocator
     */
    private RelocationHelper relocator;

    /**
     * Lazily-initialized helper for transitive dependencies resolution
     */
    private TransitiveDependencyHelper transitiveDependencyHelper;

    /**
     * Lazily-initialized helper used to fetch the JSON config
     */
    private ConfigurationFetcher configurationFetcher;

    /**
     * Global isolated class loader for libraries
     */
    private final IsolatedClassLoader globalIsolatedClassLoader = new IsolatedClassLoader();

    /**
     * Map of isolated class loaders and theirs id
     */
    private final Map<String, IsolatedClassLoader> isolatedLibraries = new HashMap<>();

    /**
     * Creates a new library manager.
     *
     * @param logAdapter    logging adapter
     * @param dataDirectory data directory
     * @deprecated Use {@link LibraryManager#LibraryManager(LogAdapter, Path, String)}
     */
    @Deprecated
    protected LibraryManager(@NotNull LogAdapter logAdapter, @NotNull Path dataDirectory) {
        logger = new Logger(requireNonNull(logAdapter, "logAdapter"));
        saveDirectory = requireNonNull(dataDirectory, "dataDirectory").toAbsolutePath().resolve("lib");
    }

    /**
     * Creates a new library manager.
     *
     * @param logAdapter    logging adapter
     * @param dataDirectory data directory
     * @param directoryName download directory name
     */
    protected LibraryManager(@NotNull LogAdapter logAdapter, @NotNull Path dataDirectory, @NotNull String directoryName) {
        logger = new Logger(requireNonNull(logAdapter, "logAdapter"));
        saveDirectory = requireNonNull(dataDirectory, "dataDirectory").toAbsolutePath().resolve(requireNonNull(directoryName, "directoryName"));
    }

    /**
     * Adds a file to the classloader classpath.
     *
     * @param file the file to add
     */
    protected abstract void addToClasspath(@NotNull Path file);

    /**
     * Adds a file to the isolated class loader
     *
     * @param library the library to add
     * @param file    the file to add
     */
    protected void addToIsolatedClasspath(@NotNull Library library, @NotNull Path file) {
        IsolatedClassLoader classLoader;
        String loaderId = library.getLoaderId();
        if (loaderId != null) {
            classLoader = isolatedLibraries.computeIfAbsent(loaderId, s -> new IsolatedClassLoader());
        } else {
            classLoader = globalIsolatedClassLoader;
        }
        classLoader.addPath(file);
    }
    
    /**
     * Get the global isolated class loader for libraries
     *
     * @return the isolated class loader
     */
    @NotNull
    public IsolatedClassLoader getGlobalIsolatedClassLoader() {
        return globalIsolatedClassLoader;
    }
    
    /**
     * Get the isolated class loader of the library
     *
     * @param loaderId the id of the loader
     * @return the isolated class loader associated with the provided id
     */
    @Nullable
    public IsolatedClassLoader getIsolatedClassLoaderById(@NotNull String loaderId) {
        return isolatedLibraries.get(loaderId);
    }

    /**
     * Gets the logging level for this library manager.
     *
     * @return log level
     */
    @NotNull
    public LogLevel getLogLevel() {
        return logger.getLevel();
    }

    /**
     * Sets the logging level for this library manager.
     * <p>
     * By setting this value, the library manager's logger will not log any
     * messages with a level less severe than the configured level. This can be
     * useful for silencing the download and relocation logging.
     * <p>
     * Setting this value to {@link LogLevel#WARN} would silence informational
     * logging but still print important things like invalid checksum warnings.
     *
     * @param level the log level to set
     */
    public void setLogLevel(@NotNull LogLevel level) {
        logger.setLevel(level);
    }

    /**
     * Gets the logger of this library manager.
     *
     * @return the logger
     */
    @NotNull
    public Logger getLogger() {
        return logger;
    }

    /**
     * Gets the currently added repositories used to resolve artifacts.
     * <p>
     * For each library this list is traversed to download artifacts after the
     * direct download URLs have been attempted.
     *
     * @return current repositories
     */
    @NotNull
    public Collection<String> getRepositories() {
        List<String> urls;
        synchronized (repositories) {
            urls = new LinkedList<>(repositories);
        }

        return Collections.unmodifiableList(urls);
    }

    /**
     * Adds a repository URL to this library manager.
     * <p>
     * Artifacts will be resolved using this repository when attempts to locate
     * the artifact through previously added repositories are all unsuccessful.
     *
     * @param url repository URL to add
     */
    public void addRepository(@NotNull String url) {
        String repo = requireNonNull(url, "url").endsWith("/") ? url : url + '/';
        synchronized (repositories) {
            repositories.add(repo);
        }
    }

    /**
     * Adds the current user's local Maven repository.
     */
    public void addMavenLocal() {
        addRepository(Paths.get(System.getProperty("user.home")).resolve(".m2/repository").toUri().toString());
    }

    /**
     * Adds the Maven Central repository.
     */
    public void addMavenCentral() {
        addRepository(Repositories.MAVEN_CENTRAL);
    }

    /**
     * Adds the Sonatype OSS repository.
     */
    public void addSonatype() {
        addRepository(Repositories.SONATYPE);
    }

    /**
     * Adds the Bintray JCenter repository.
     */
    public void addJCenter() {
        addRepository(Repositories.JCENTER);
    }

    /**
     * Adds the JitPack repository.
     */
    public void addJitPack() {
        addRepository(Repositories.JITPACK);
    }

    /**
     * Gets all the possible download URLs for this library. Entries are
     * ordered by direct download URLs first and then repository download URLs.
     * <br>This method also resolves SNAPSHOT artifacts URLs.
     *
     * @param library the library to resolve
     * @return download URLs
     */
    @NotNull
    public Collection<String> resolveLibrary(@NotNull Library library) {
        Set<String> urls = new LinkedHashSet<>(requireNonNull(library, "library").getUrls());
        boolean snapshot = library.isSnapshot();

        // Try from library-declared repos first
        for (String repository : library.getRepositories()) {
            if (snapshot) {
                String url = resolveSnapshot(repository, library);
                if (url != null)
                    urls.add(repository + url);
            } else {
                urls.add(repository + library.getPath());
            }
        }

        for (String repository : getRepositories()) {
            if (snapshot) {
                String url = resolveSnapshot(repository, library);
                if (url != null)
                    urls.add(repository + url);
            } else {
                urls.add(repository + library.getPath());
            }
        }

        return Collections.unmodifiableSet(urls);
    }

    /**
     * Resolves the URL of the artifact of a snapshot library.
     *
     * @param repository The repository to query for snapshot information
     * @param library    The library
     * @return The URl of the artifact of a snapshot library or null if no information could be gathered from the
     * provided repository
     */
    @Nullable
    private String resolveSnapshot(@NotNull String repository, @NotNull Library library) {
        String url = requireNonNull(repository, "repository") + requireNonNull(library, "library").getPartialPath() + "maven-metadata.xml";
        try {
            URLConnection connection = new URL(requireNonNull(url, "url")).openConnection();

            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", LibbyProperties.HTTP_USER_AGENT);

            try (InputStream in = connection.getInputStream()) {
                return getURLFromMetadata(in, library);
            }
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        } catch (IOException e) {
            if (e instanceof FileNotFoundException) {
                logger.debug("File not found: " + url);
            } else if (e instanceof SocketTimeoutException) {
                logger.debug("Connect timed out: " + url);
            } else if (e instanceof UnknownHostException) {
                logger.debug("Unknown host: " + url);
            } else {
                logger.debug("Unexpected IOException", e);
            }

            return null;
        }
    }

    /**
     * Gets the URL of the artifact of a snapshot library from the provided InputStream, which should be opened to the
     * library's maven-metadata.xml.
     *
     * @param inputStream The InputStream opened to the library's maven-metadata.xml
     * @param library     The library
     * @return The URl of the artifact of a snapshot library or null if no information could be gathered from the
     * provided inputStream
     * @throws IOException If any IO errors occur
     */
    @Nullable
    private String getURLFromMetadata(@NotNull InputStream inputStream, @NotNull Library library) throws IOException {
        requireNonNull(inputStream, "inputStream");
        requireNonNull(library, "library");

        String timestamp, buildNumber;
        try {
            // This reads the maven-metadata.xml file and gets the snapshot info from the <snapshot> tag.
            // Example tag:
            // <snapshot>
            //     <timestamp>20220617.013635</timestamp>
            //     <buildNumber>12</buildNumber>
            // </snapshot>

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputStream);
            doc.getDocumentElement().normalize();

            NodeList nodes = doc.getElementsByTagName("snapshot");
            if (nodes.getLength() == 0) {
                return null;
            }
            Node snapshot = nodes.item(0);
            if (snapshot.getNodeType() != Node.ELEMENT_NODE) {
                return null;
            }
            Node timestampNode = ((Element) snapshot).getElementsByTagName("timestamp").item(0);
            if (timestampNode == null || timestampNode.getNodeType() != Node.ELEMENT_NODE) {
                return null;
            }
            Node buildNumberNode = ((Element) snapshot).getElementsByTagName("buildNumber").item(0);
            if (buildNumberNode == null || buildNumberNode.getNodeType() != Node.ELEMENT_NODE) {
                return null;
            }
            Node timestampChild = timestampNode.getFirstChild();
            if (timestampChild == null || timestampChild.getNodeType() != Node.TEXT_NODE) {
                return null;
            }
            Node buildNumberChild = buildNumberNode.getFirstChild();
            if (buildNumberChild == null || buildNumberChild.getNodeType() != Node.TEXT_NODE) {
                return null;
            }
            timestamp = timestampChild.getNodeValue();
            buildNumber = buildNumberChild.getNodeValue();
        } catch (ParserConfigurationException | SAXException e) {
            logger.debug("Invalid maven-metadata.xml", e);
            return null;
        }

        String version = library.getVersion();
        // Call .substring(...) only on versions ending in "-SNAPSHOT".
        // It should never happen that a snapshot version doesn't end in "-SNAPSHOT", but better be sure
        if (version.endsWith("-SNAPSHOT")) {
            version = version.substring(0, version.length() - "-SNAPSHOT".length());
        }

        String url = library.getPartialPath() + library.getArtifactId() + '-' + version + '-' + timestamp + '-' + buildNumber;
        if (library.hasClassifier()) {
            url += '-' + library.getClassifier();
        }
        return url + ".jar";
    }

    /**
     * Downloads a library jar and returns the contents as a byte array.
     *
     * @param url the URL to the library jar
     * @return downloaded jar as byte array or null if nothing was downloaded
     */
    private byte[] downloadLibrary(@NotNull String url) {
        try {
            URLConnection connection = new URL(requireNonNull(url, "url")).openConnection();

            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", LibbyProperties.HTTP_USER_AGENT);

            try (InputStream in = connection.getInputStream()) {
                int len;
                byte[] buf = new byte[8192];
                ByteArrayOutputStream out = new ByteArrayOutputStream();

                try {
                    while ((len = in.read(buf)) != -1) {
                        out.write(buf, 0, len);
                    }
                } catch (SocketTimeoutException e) {
                    logger.warn("Download timed out: " + connection.getURL());
                    return null;
                }

                logger.info("Downloaded library " + connection.getURL());
                return out.toByteArray();
            }
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        } catch (IOException e) {
            if (e instanceof FileNotFoundException) {
                logger.debug("File not found: " + url);
            } else if (e instanceof SocketTimeoutException) {
                logger.debug("Connect timed out: " + url);
            } else if (e instanceof UnknownHostException) {
                logger.debug("Unknown host: " + url);
            } else {
                logger.debug("Unexpected IOException", e);
            }

            return null;
        }
    }

    /**
     * Downloads a library jar to the save directory if it doesn't already
     * exist (snapshot libraries are always re-downloaded) and returns
     * the local file path.
     * <p>
     * If the library has a checksum, it will be compared against the
     * downloaded jar's checksum to verify the integrity of the download. If
     * the checksums don't match, a warning is generated and the next download
     * URL is attempted.
     * <p>
     * Checksum comparison is ignored if the library doesn't have a checksum
     * or if the library jar already exists in the save directory.
     * <p>
     * Most of the time it is advised to use {@link #loadLibrary(Library)}
     * instead of this method because this one is only concerned with
     * downloading the jar and returning the local path. It's usually more
     * desirable to download the jar and add it to the classloader's classpath in
     * one operation.
     * <p>
     * Once the library is downloaded, relocations are applied automatically and
     * its returned the path to the relocated jar.
     *
     * @param library the library to download
     * @return local file path to library
     * @see #loadLibrary(Library)
     * @see #relocate(Path, String, Collection)
     */
    @NotNull
    public Path downloadLibrary(@NotNull Library library) {
        Path file = saveDirectory.resolve(requireNonNull(library, "library").getPath());
        if (Files.exists(file)) {
            // Early return only if library isn't a snapshot, since snapshot libraries are always re-downloaded
            if (!library.isSnapshot()) {
                // Relocate the file
                if (library.hasRelocations()) {
                    file = relocate(file, library.getRelocatedPath(), library.getRelocations());
                }

                return file;
            }

            // Delete the file since the Files.move call down below will fail if it exists
            try {
                Files.delete(file);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        Collection<String> urls = resolveLibrary(library);
        if (urls.isEmpty()) {
            throw new RuntimeException("Library '" + library + "' couldn't be resolved, add a repository");
        }

        MessageDigest md = null;
        if (library.hasChecksum()) {
            try {
                md = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

        Path out = file.resolveSibling(file.getFileName() + ".tmp");
        out.toFile().deleteOnExit();

        try {
            Files.createDirectories(file.getParent());

            for (String url : urls) {
                byte[] bytes = downloadLibrary(url);
                if (bytes == null) {
                    continue;
                }

                if (md != null) {
                    byte[] checksum = md.digest(bytes);
                    if (!Arrays.equals(checksum, library.getChecksum())) {
                        logger.warn("*** INVALID CHECKSUM ***");
                        logger.warn(" Library :  " + library);
                        logger.warn(" URL :  " + url);
                        logger.warn(" Expected :  " + Base64.getEncoder().encodeToString(library.getChecksum()));
                        logger.warn(" Actual :  " + Base64.getEncoder().encodeToString(checksum));
                        continue;
                    }
                }

                Files.write(out, bytes);
                Files.move(out, file);

                // Relocate the file
                if (library.hasRelocations()) {
                    file = relocate(file, library.getRelocatedPath(), library.getRelocations());
                }

                return file;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            try {
                Files.deleteIfExists(out);
            } catch (IOException ignored) {
            }
        }

        throw new RuntimeException("Failed to download library '" + library + "'");
    }

    /**
     * Processes the input jar and generates an output jar with the provided
     * relocation rules applied, then returns the path to the relocated jar.
     *
     * @param in          input jar
     * @param out         output jar
     * @param relocations relocations to apply
     * @return the relocated file
     * @see RelocationHelper#relocate(Path, Path, Collection)
     */
    @NotNull
    public Path relocate(@NotNull Path in, @NotNull String out, @NotNull Collection<Relocation> relocations) {
        requireNonNull(in, "in");
        requireNonNull(out, "out");
        requireNonNull(relocations, "relocations");

        Path file = saveDirectory.resolve(out);
        if (Files.exists(file)) {
            return file;
        }

        Path tmpOut = file.resolveSibling(file.getFileName() + ".tmp");
        tmpOut.toFile().deleteOnExit();

        synchronized (this) {
            if (relocator == null) {
                relocator = new RelocationHelper(this);
            }
        }

        try {
            relocator.relocate(in, tmpOut, relocations);
            Files.move(tmpOut, file);

            logger.info("Relocations applied to " + saveDirectory.getParent().relativize(in));

            return file;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            try {
                Files.deleteIfExists(tmpOut);
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Resolves and loads transitive libraries for a given library. This method ensures that
     * all libraries on which the provided library depends are properly loaded.
     *
     * @param library the primary library for which transitive libraries need to be resolved and loaded.
     * @throws NullPointerException if the provided library is null.
     * @see #loadLibrary(Library)
     */
    private void resolveTransitiveLibraries(@NotNull Library library) {
        requireNonNull(library, "library");

        synchronized (this) {
            if (transitiveDependencyHelper == null) {
                transitiveDependencyHelper = new TransitiveDependencyHelper(this, saveDirectory);
            }
        }

        for (Library transitiveLibrary : transitiveDependencyHelper.findTransitiveLibraries(library)) {
            loadLibrary(transitiveLibrary);
        }
    }

    /**
     * Loads a library jar into the classloader classpath. If the library jar
     * doesn't exist locally, it will be downloaded.
     * <p>
     * If the provided library has any relocations, they will be applied to
     * create a relocated jar and the relocated jar will be loaded instead.
     *
     * @param library the library to load
     * @see #downloadLibrary(Library)
     */
    public void loadLibrary(@NotNull Library library) {
        Path file = downloadAndRelocate(requireNonNull(library, "library"));
        Path file = downloadLibrary(requireNonNull(library, "library"));
        if (library.resolveTransitiveDependencies()) {
            resolveTransitiveLibraries(library);
        }

        if (library.isIsolatedLoad()) {
            addToIsolatedClasspath(library, file);
        } else {
            addToClasspath(file);
        }
    }

    /**
     * Loads multiple libraries into the classloader classpath.
     *
     * @param libraries the libraries to load
     * @see #loadLibrary(Library)
     */
    public void loadLibraries(@NotNull Library... libraries) {
        for (Library library : libraries) {
            loadLibrary(library);
        }
    }

    /**
     * Configures the current library manager from a libby.json file in the classloader classpath.
     *
     * @throws ConfigurationException If the configuration contained an error
     * @throws MalformedConfigurationException If the provided JSON file contained a syntactic error or couldn't be read
     */
    public void configureFromJSON() {
        configureFromJSON("libby.json");
    }

    /**
     * Configures the current library manager from a file in the classloader classpath.
     * Example: configureFromJSON("libby.json")
     *
     * @param fileName the name of the json file
     * @throws ConfigurationException If the configuration contained an error
     * @throws MalformedConfigurationException If the provided JSON file contained a syntactic error or couldn't be read
     */
    public void configureFromJSON(@NotNull String fileName) {
        configureFromJSON(requireNonNull(getResourceAsStream(fileName), "resourceAsStream"));
    }

    /**
     * Configures the current library manager from a json file.
     *
     * @param data the json file
     * @throws ConfigurationException If the configuration contained an error
     * @throws MalformedConfigurationException If the provided JSON contained a syntactic error or couldn't be read
     */
    public void configureFromJSON(@NotNull InputStream data) {
        synchronized (this) {
            if (configurationFetcher == null) {
                configurationFetcher = new ConfigurationFetcher(this);
            }
        }

        Configuration config = configurationFetcher.readJsonFile(data);

        // Load repositories
        for (String repo : config.getRepositories()) {
            addRepository(repo);
        }

        // Load libraries
        for (Library library : config.getLibraries()) {
            loadLibrary(library);
        }
    }

    /**
     * Returns an input stream for reading the specified resource.
     *
     * @param path the path to the resource
     * @return input stream for the resource
     */
    @Nullable
    protected InputStream getResourceAsStream(@NotNull String path) {
        return getClass().getClassLoader().getResourceAsStream(path);
    }
}
