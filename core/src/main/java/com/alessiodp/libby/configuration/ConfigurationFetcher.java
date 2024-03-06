package com.alessiodp.libby.configuration;

import com.alessiodp.libby.Library;
import com.alessiodp.libby.LibraryManager;
import com.alessiodp.libby.Repositories;
import com.alessiodp.libby.classloader.IsolatedClassLoader;
import com.alessiodp.libby.relocation.Relocation;
import com.alessiodp.libby.transitive.ExcludedDependency;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.alessiodp.libby.Util.replaceWithDots;
import static java.util.Objects.requireNonNull;

/**
 * This class is used to fetch the JSON configuration file.
 */
public class ConfigurationFetcher {
    // IMPLEMENTATION NOTE:
    // JsonObject extends Map<String, Object>
    // JsonArray extends ArrayList<Object>
    // To avoid more reflections, the types Map<String, Object> and ArrayList<Object> are used instead of the Json* ones

    /**
     * The supported version of the configuration.
     */
    public static final int CONFIGURATION_VERSION = 0;

    /**
     * com.grack.nanojson.JsonParser class name for reflections
     */
    private static final String JSON_PARSER_CLASS = replaceWithDots("com{}grack{}nanojson{}JsonParser");

    /**
     * com.grack.nanojson.JsonParser.JsonParserContext class name for reflections
     */
    private static final String JSON_PARSER_CONTEXT_CLASS = replaceWithDots("com{}grack{}nanojson{}JsonParser$JsonParserContext");

    /**
     * com.grack.nanojson.JsonParserException class name for reflections
     */
    private static final String JSON_PARSER_EXCEPTION_CLASS = replaceWithDots("com{}grack{}nanojson{}JsonParserException");

    /**
     * com.grack.nanojson.JsonObject class name for reflections
     */
    private static final String JSON_OBJECT_CLASS = replaceWithDots("com{}grack{}nanojson{}JsonObject");

    /**
     * com.grack.nanojson.JsonArray class name for reflections
     */
    private static final String JSON_ARRAY_CLASS = replaceWithDots("com{}grack{}nanojson{}JsonArray");

    /**
     * com.grack.nanojson.JsonParser#object() method
     */
    private final Method jsonParserObject;

    /**
     * com.grack.nanojson.JsonParser.JsonParserContext#from(InputStream) method
     */
    private final Method jsonParserFrom;

    /**
     * com.grack.nanojson.JsonObject getter methods
     */
    private final Method jsonObjectGetArray, jsonObjectGetBoolean, jsonObjectGetString;

    /**
     * com.grack.nanojson.JsonArray#getObject(int) method
     */
    private final Method jsonArrayGetObject;

    /**
     * com.grack.nanojson.JsonParserException class
     */
    private final Class<?> jsonParserException;

    /**
     * Creates a new configuration fetcher using the provided library manager to
     * download the dependencies required for reading the configuration.
     *
     * @param libraryManager the library manager used to read the configuration
     */
    public ConfigurationFetcher(@NotNull LibraryManager libraryManager) {
        requireNonNull(libraryManager, "libraryManager");

        IsolatedClassLoader classLoader = new IsolatedClassLoader();

        // NanoJson
        classLoader.addPath(libraryManager.downloadLibrary(
                Library.builder()
                        .groupId("com{}grack")
                        .artifactId("nanojson")
                        .version("1.8")
                        .checksumFromBase64("qyhAVZM8LYvqhGQrbmW2aHV4hRzn+2flPCV98wAimJo=")
                        .repository(Repositories.MAVEN_CENTRAL)
                        .build()
        ));

        try {
            Class<?> jsonParser = classLoader.loadClass(JSON_PARSER_CLASS);
            Class<?> jsonParserContext = classLoader.loadClass(JSON_PARSER_CONTEXT_CLASS);
            Class<?> jsonObject = classLoader.loadClass(JSON_OBJECT_CLASS);
            Class<?> jsonArray = classLoader.loadClass(JSON_ARRAY_CLASS);
            jsonParserException = classLoader.loadClass(JSON_PARSER_EXCEPTION_CLASS);

            // com.grack.nanojson.JsonParser#object()
            jsonParserObject = jsonParser.getMethod("object");

            // com.grack.nanojson.JsonParser.JsonParserContext#from(InputStream)
            jsonParserFrom = jsonParserContext.getMethod("from", InputStream.class);

            // com.grack.nanojson.JsonObject#getArray(String)
            jsonObjectGetArray = jsonObject.getMethod("getArray", String.class);

            // com.grack.nanojson.JsonObject#getBoolean(String)
            jsonObjectGetBoolean = jsonObject.getMethod("getBoolean", String.class);

            // com.grack.nanojson.JsonObject#getString(String)
            jsonObjectGetString = jsonObject.getMethod("getString", String.class);

            // com.grack.nanojson.JsonArray#getObject(int)
            jsonArrayGetObject = jsonArray.getMethod("getObject", int.class);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads the provided JSON configuration and returns a {@link Configuration} object containing the configuration.
     *
     * @param data The InputStream containing the JSON configuration
     * @return The read {@link Configuration}
     * @throws ConfigurationException If the configuration contained an error
     * @throws MalformedConfigurationException If the provided JSON contained a syntactic error or couldn't be read
     */
    @SuppressWarnings("unchecked")
    public Configuration readJsonFile(@NotNull InputStream data) {
        try {
            Map<String, Object> root;
            try {
                root = (Map<String, Object>) jsonParserFrom.invoke(jsonParserObject.invoke(null), data);
            } catch (InvocationTargetException e) {
                if (jsonParserException.isInstance(e.getCause())) {
                    throw new MalformedConfigurationException(e.getCause().getMessage(), e.getCause());
                }
                throw new RuntimeException(e);
            }

            Integer version = fetchVersion(root);
            Set<String> repositories = fetchRepositories(root);
            Set<Relocation> globalRelocations = fetchRelocations(root);
            List<Library> libraries = fetchLibraries(root, globalRelocations);

            return new Configuration(version, repositories, globalRelocations, libraries);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Fetch the version of the JSON file and, if exists, it must match the version of the parser
     *
     * @param configuration the root object of the JSON file
     * @return the fetched version or null if not found
     */
    private Integer fetchVersion(@NotNull Map<String, Object> configuration) {
        Object version = configuration.get("version");

        if (version instanceof Number) {
            int ver = ((Number) version).intValue();

            if (ver != CONFIGURATION_VERSION) {
                throw new ConfigurationException("The json file is version " + version + " but this version of libby only supports version " + CONFIGURATION_VERSION);
            }

            return ver;
        }

        return null;
    }

    /**
     * Fetch the repositories from the JSON file. It can be omitted from the JSON.
     * If defined, it must be an array of string representing the repository URLs.
     *
     * @param configuration the root object of the JSON file
     * @return the set of repositories as strings
     */
    private Set<String> fetchRepositories(@NotNull Map<String, Object> configuration) throws ReflectiveOperationException {
        Set<String> repos = new HashSet<>();
        ArrayList<Object> repositories = getArray(configuration, "repositories");
        if (repositories != null) {
            for (Object repository : repositories) {
                if (repository instanceof String) {
                    repos.add((String) repository);
                } else {
                    throw new ConfigurationException("Invalid repository: " + repository);
                }
            }
        }
        return repos;
    }

    /**
     * Fetch the relocations from the JSON file. It can be omitted from the JSON.
     * If defined, they must be an array of objects that include the following properties:
     * <ul>
     *     <li>pattern: The search pattern</li>
     *     <li>relocatedPattern: The replacement pattern</li>
     * </ul>
     * Optional properties:
     * <ul>
     *     <li>includes: An array of strings representing the classes and resources to include</li>
     *     <li>excludes: An array of strings representing the classes and resources to exclude</li>
     * </ul>
     *
     * @param configuration the root object of the JSON file
     * @return The set of relocations
     */
    @SuppressWarnings("unchecked")
    private Set<Relocation> fetchRelocations(@NotNull Map<String, Object> configuration) throws ReflectiveOperationException {
        ArrayList<Object> relocations = getArray(configuration, "relocations");

        if (relocations != null) {
            Set<Relocation> fetchedRelocations = new HashSet<>();

            for (int i = 0; i < relocations.size(); i++) {
                Map<String, Object> relocation = getObject(relocations, i);

                if (relocation == null) {
                    // Use relocations.get(i) to get the best possible error message, since null means it isn't a JsonObject
                    throw new ConfigurationException("Invalid relocation: " + relocations.get(i));
                }

                String pattern = getString(relocation, "pattern");

                if (pattern == null) {
                    throw new ConfigurationException("The pattern property is required for all relocations");
                }

                String relocatedPattern = getString(relocation, "relocatedPattern");

                if (relocatedPattern == null) {
                    throw new ConfigurationException("The relocatedPattern property is required for all relocations");
                }

                ArrayList<?> includes = getArray(relocation, "includes");
                // Just check if every element is a non-null String
                if (includes != null) {
                    for (Object include : includes) {
                        if (!(include instanceof String)) {
                            throw new ConfigurationException("Invalid relocation include: " + include);
                        }
                    }
                }

                ArrayList<?> excludes = getArray(relocation, "excludes");
                // Just check if every element is a non-null String
                if (excludes != null) {
                    for (Object exclude : excludes) {
                        if (!(exclude instanceof String)) {
                            throw new ConfigurationException("Invalid relocation exclude: " + exclude);
                        }
                    }
                }

                fetchedRelocations.add(new Relocation(pattern, relocatedPattern, (Collection<String>) includes, (Collection<String>) excludes));
            }

            return Collections.unmodifiableSet(fetchedRelocations);
        }

        return Collections.emptySet();
    }

    /**
     * Fetch the excluded transitive dependencies from the JSON of a library. It can be omitted.
     * If defined, they must be an array of objects that include the following properties:
     * <ul>
     *     <li>groupId: The groupId of the excluded dependency</li>
     *     <li>artifactId: The artifactId excluded dependency</li>
     * </ul>
     *
     * @param library The JsonObject of the library
     * @return The set containing the excluded dependencies of the library
     */
    @NotNull
    private Set<ExcludedDependency> fetchExcludedTransitiveDependencies(@NotNull Map<String, Object> library) throws ReflectiveOperationException {
        ArrayList<Object> excludedDependencies = getArray(library, "excludedTransitiveDependencies");

        if (excludedDependencies != null) {
            Set<ExcludedDependency> fetchedExcludedDependencies = new HashSet<>();

            for (int i = 0; i < excludedDependencies.size(); i++) {
                Map<String, Object> excludedDependency = getObject(excludedDependencies, i);

                if (excludedDependency == null) {
                    // Use excludedDependencies.get(i) to get the best possible error message, since null means it isn't a JsonObject
                    throw new ConfigurationException("Invalid excluded transitive dependency: " + excludedDependencies.get(i));
                }

                String groupId = getString(excludedDependency, "groupId");

                if (groupId == null) {
                    throw new ConfigurationException("The groupId property is required for all excluded transitive dependencies");
                }

                String artifactId = getString(excludedDependency, "artifactId");

                if (artifactId == null) {
                    throw new ConfigurationException("The artifactId property is required for all excluded transitive dependencies");
                }

                fetchedExcludedDependencies.add(new ExcludedDependency(groupId, artifactId));
            }

            return Collections.unmodifiableSet(fetchedExcludedDependencies);
        }

        return Collections.emptySet();
    }

    /**
     * Fetch the libraries from the JSON file. It can be omitted from the JSON.
     * If defined, they must be an array of objects that include the following properties:
     * <ul>
     *     <li>groupId: The groupId of the library</li>
     *     <li>artifactId: The artifactId of the library</li>
     *     <li>version: The version of the library</li>
     * </ul>
     * Optional properties:
     * <ul>
     *     <li>checksum: The SHA-256 checksum of the library, may only be included if the library is a JAR</li>
     *     <li>checksumFromBase64: The SHA-256 checksum of the library, must be a base64 encoded string and may only be included if the library is a JAR</li>
     *     <li>classifier: The artifact classifier of the library</li>
     *     <li>isolatedLoad: Whether to load this library in an IsolatedClassLoader</li>
     *     <li>loaderId: The loader ID of this library</li>
     *     <li>repositories: An array of additional per-library repositories</li>
     *     <li>relocations: An array of relocations to apply to this library</li>
     *     <li>resolveTransitiveDependencies: Whether to resolve transitive dependencies</li>
     *     <li>excludedTransitiveDependencies: An array of dependencies excluded during transitive dependencies resolution</li>
     * </ul>
     *
     * @param configuration the root object of the JSON file
     * @param globalRelocations the set of global relocations to apply to all libraries
     * @return The list of libraries fetched from the JSON file
     */
    private List<Library> fetchLibraries(@NotNull Map<String, Object> configuration, @NotNull Set<Relocation> globalRelocations) throws ReflectiveOperationException {
        ArrayList<Object> libraries = getArray(configuration, "libraries");

        if (libraries != null) {
            List<Library> fetchedLibraries = new ArrayList<>(libraries.size());

            for (int i = 0; i < libraries.size(); i++) {
                Map<String, Object> library = getObject(libraries, i);

                if (library == null) {
                    // Use libraries.get(i) to get the best possible error message, since null means it isn't a JsonObject
                    throw new ConfigurationException("Invalid library: " + libraries.get(i));
                }

                Library.Builder libraryBuilder = Library.builder();

                String groupId = getString(library, "groupId");

                if (groupId == null) {
                    throw new ConfigurationException("The groupId property is required for all libraries");
                }

                String artifactId = getString(library, "artifactId");

                if (artifactId == null) {
                    throw new ConfigurationException("The artifactId property is required for all libraries");
                }

                String artifactVersion = getString(library, "version");

                if (artifactVersion == null) {
                    throw new ConfigurationException("The version property is required for all libraries");
                }

                libraryBuilder
                        .groupId(groupId)
                        .artifactId(artifactId)
                        .version(artifactVersion);

                String checksum = getString(library, "checksum");

                if (checksum != null) {
                    libraryBuilder.checksum(checksum);
                }

                String checksumFromBase64 = getString(library, "checksumFromBase64");

                if (checksumFromBase64 != null) {
                    try {
                        libraryBuilder.checksumFromBase64(checksumFromBase64);
                    } catch (IllegalArgumentException ignored) {
                        throw new ConfigurationException("The checksum property must be a valid base64 encoded SHA-256 checksum");
                    }
                }

                libraryBuilder.isolatedLoad(getBoolean(library, "isolatedLoad"));

                libraryBuilder.loaderId(getString(library, "loaderId"));

                libraryBuilder.classifier(getString(library, "classifier"));

                libraryBuilder.resolveTransitiveDependencies(getBoolean(library, "resolveTransitiveDependencies"));

                fetchExcludedTransitiveDependencies(library).forEach(libraryBuilder::excludeTransitiveDependency);

                fetchRepositories(library).forEach(libraryBuilder::repository);

                Set<Relocation> relocations = fetchRelocations(library);

                // Apply relocation
                for (Relocation relocation : relocations) {
                    libraryBuilder.relocate(relocation);
                }

                // Apply global defined relocations
                for (Relocation relocation : globalRelocations) {
                    libraryBuilder.relocate(relocation);
                }

                fetchedLibraries.add(libraryBuilder.build());
            }

            return Collections.unmodifiableList(fetchedLibraries);
        }

        return Collections.emptyList();
    }

    private boolean getBoolean(@NotNull Map<String, Object> jsonObject, @NotNull String key) throws ReflectiveOperationException {
        return (boolean) jsonObjectGetBoolean.invoke(jsonObject, key);
    }

    @Nullable
    private String getString(@NotNull Map<String, Object> jsonObject, @NotNull String key) throws ReflectiveOperationException {
        return (String) jsonObjectGetString.invoke(jsonObject, key);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private ArrayList<Object> getArray(@NotNull Map<String, Object> jsonObject, @NotNull String key) throws ReflectiveOperationException {
        return (ArrayList<Object>) jsonObjectGetArray.invoke(jsonObject, key);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private Map<String, Object> getObject(@NotNull ArrayList<Object> jsonArray, int index) throws ReflectiveOperationException {
        return (Map<String, Object>) jsonArrayGetObject.invoke(jsonArray, index);
    }
}
