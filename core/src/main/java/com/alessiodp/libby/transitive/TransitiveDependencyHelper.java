package com.alessiodp.libby.transitive;

import com.alessiodp.libby.Library;
import com.alessiodp.libby.LibraryManager;
import com.alessiodp.libby.classloader.IsolatedClassLoader;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * A reflection-based helper for resolving transitive libraries. It automatically
 * downloads Maven Resolver Supplier, Maven Resolver Provider and their transitive dependencies to resolve transitive dependencies.
 *
 * @see <a href="https://github.com/apache/maven-resolver">Apache Maven Artifact Resolver</a>
 */
public class TransitiveDependencyHelper {
    /**
     * TransitiveDependencyCollector class instance, used in {@link #findTransitiveLibraries(Library)}
     */
    private final Object transitiveDependencyCollectorObject;

    /**
     * Reflected method for resolving transitive dependencies
     */
    private final Method resolveTransitiveDependenciesMethod;

    /**
     * Reflected getter methods of Artifact class
     */
    private final Method artifactGetGroupIdMethod, artifactGetArtifactIdMethod, artifactGetVersionMethod;

    /**
     * LibraryManager instance, used in {@link #findTransitiveLibraries(Library)}
     */
    private final LibraryManager libraryManager;

    /**
     * Creates a new transitive dependency helper using the provided library manager to
     * download the dependencies required for transitive dependency resolvement in runtime.
     *
     * @param libraryManager the library manager used to download dependencies
     * @param saveDirectory  the directory where all transitive dependencies would be saved
     */
    public TransitiveDependencyHelper(LibraryManager libraryManager, Path saveDirectory) {
        requireNonNull(libraryManager, "libraryManager");
        this.libraryManager = libraryManager;

        IsolatedClassLoader classLoader = new IsolatedClassLoader();
        String collectorClassName = "com.alessiodp.libby.transitive.TransitiveDependencyCollector";
        String collectorClassPath = '/' + collectorClassName.replace('.', '/') + ".class";

        for (TransitiveLibraryResolutionDependency dependency : TransitiveLibraryResolutionDependency.values())
            classLoader.addPath(libraryManager.downloadLibrary(dependency.toLibrary()));

        final Class<?> transitiveDependencyCollectorClass;
        try {
            transitiveDependencyCollectorClass = classLoader.defineClass(collectorClassName, getClass().getResourceAsStream(collectorClassPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            Class<?> artifactClass = classLoader.loadClass("org.eclipse.aether.artifact.Artifact");

            // com.alessiodp.libby.TransitiveDependencyCollector(Path)
            Constructor<?> constructor = transitiveDependencyCollectorClass.getConstructor(Path.class);
            constructor.setAccessible(true);
            transitiveDependencyCollectorObject = constructor.newInstance(saveDirectory);
            // com.alessiodp.libby.TransitiveDependencyCollector#findTransitiveDependencies(String, String, String, String...)
            resolveTransitiveDependenciesMethod = transitiveDependencyCollectorClass.getMethod("findTransitiveDependencies", String.class, String.class, String.class, String[].class);
            resolveTransitiveDependenciesMethod.setAccessible(true);
            // org.eclipse.aether.artifact.Artifact#getGroupId()
            artifactGetGroupIdMethod = artifactClass.getMethod("getGroupId");
            // org.eclipse.aether.artifact.Artifact#getArtifactId()
            artifactGetArtifactIdMethod = artifactClass.getMethod("getArtifactId");
            // org.eclipse.aether.artifact.Artifact#getVersion()
            artifactGetVersionMethod = artifactClass.getMethod("getVersion");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Finds and returns a collection of transitive libraries for a given library.
     * <p>
     * This method fetches the transitive dependencies of the provided library using reflection-based
     * interaction with the underlying transitive dependency collector. The method ensures to filter out
     * any excluded transitive dependencies as specified by the provided library.
     * </p>
     * <p>
     * Note: The method merges the repositories from both the library manager and the given library
     * for dependency resolution. And clones all relocations into transitive libraries.
     * </p>
     *
     * @param library The primary library for which transitive dependencies need to be found.
     * @return A collection of {@link Library} objects representing the transitive libraries
     * excluding the ones marked as excluded in the provided library.
     * @throws RuntimeException If there's any exception during the reflection-based operations.
     */
    public Collection<Library> findTransitiveLibraries(Library library) {
        List<Library> transitiveLibraries = new ArrayList<>();

        String[] repositories = Stream.of(libraryManager.getRepositories(), library.getRepositories()).flatMap(Collection::stream).toArray(String[]::new);
        try {
            Collection<?> artifacts = (Collection<?>) resolveTransitiveDependenciesMethod.invoke(transitiveDependencyCollectorObject,
                library.getGroupId(),
                library.getArtifactId(),
                library.getVersion(),
                repositories);
            for (Object artifact : artifacts) {
                String groupId = (String) artifactGetGroupIdMethod.invoke(artifact);
                String artifactId = (String) artifactGetArtifactIdMethod.invoke(artifact);
                String version = (String) artifactGetVersionMethod.invoke(artifact);

                if (library.getGroupId().equals(groupId) && library.getArtifactId().equals(artifactId)) continue;

                Library.Builder libraryBuilder = Library.builder()
                                                        .groupId(groupId)
                                                        .artifactId(artifactId)
                                                        .version(version)
                                                        .isolatedLoad(library.isIsolatedLoad())
                                                        .id(library.getId());

                library.getRelocations().forEach(libraryBuilder::relocate);
                library.getRepositories().forEach(libraryBuilder::repository);

                transitiveLibraries.add(libraryBuilder.build());
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

        return transitiveLibraries.stream().filter(transitiveLibrary -> library.getExcludedTransitiveDependencies()
                                                                               .stream()
                                                                               .noneMatch(excludedDependency -> excludedDependency.similar(transitiveLibrary)))
                                  .collect(Collectors.toList());
    }
}
