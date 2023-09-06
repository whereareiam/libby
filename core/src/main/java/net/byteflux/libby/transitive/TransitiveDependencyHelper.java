package net.byteflux.libby.transitive;

import net.byteflux.libby.Library;
import net.byteflux.libby.LibraryManager;
import net.byteflux.libby.classloader.IsolatedClassLoader;

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

public class TransitiveDependencyHelper {

    private final Object transitiveDependencyCollectorObject;
    private final Method resolveTransitiveDependenciesMethod;
    private final Method artifactGetGroupIdMethod, artifactGetArtifactIdMethod, artifactGetVersionMethod;
    private final LibraryManager libraryManager;

    public TransitiveDependencyHelper(LibraryManager libraryManager, Path saveDirectory) {
        requireNonNull(libraryManager, "libraryManager");
        this.libraryManager = libraryManager;

        IsolatedClassLoader classLoader = new IsolatedClassLoader();
        String collectorClassName = "net.byteflux.libby.transitive.TransitiveDependencyCollector";
        String collectorClassPath = '/' + collectorClassName.replace('.', '/') + ".class";

        try {
            classLoader.defineClass(collectorClassName, getClass().getResourceAsStream(collectorClassPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (Library library : TransitiveLibraryBundle.DEPENDENCY_BUNDLE)
            classLoader.addPath(libraryManager.downloadLibrary(library));

        try {
            Class<?> transitiveDependencyCollectorClass = classLoader.loadClass(collectorClassName);
            Class<?> artifactClass = classLoader.loadClass("org.eclipse.aether.artifact.Artifact");

            Constructor<?> constructor = transitiveDependencyCollectorClass.getConstructor(Path.class);
            constructor.setAccessible(true);
            transitiveDependencyCollectorObject = constructor.newInstance(saveDirectory);
            resolveTransitiveDependenciesMethod = transitiveDependencyCollectorClass.getMethod("findTransitiveDependencies", String.class, String.class, String.class, String[].class);
            resolveTransitiveDependenciesMethod.setAccessible(true);
            artifactGetGroupIdMethod = artifactClass.getMethod("getGroupId");
            artifactGetArtifactIdMethod = artifactClass.getMethod("getArtifactId");
            artifactGetVersionMethod = artifactClass.getMethod("getVersion");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public Collection<Library> findTransitiveLibraries(Library library) {
        List<Library> transitiveLibraries = new ArrayList<>();

        String[] repositories = Stream.of(libraryManager.getRepositories(), library.getRepositories()).flatMap(Collection::stream).toArray(String[]::new);
        try {
            Collection<Object> artifacts = (Collection<Object>) resolveTransitiveDependenciesMethod.invoke(transitiveDependencyCollectorObject,
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
                                                        .version(version);

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
