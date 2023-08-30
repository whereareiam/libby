package net.byteflux.libby;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyResolutionException;

import net.byteflux.libby.helper.TransitiveDependencyHelper;
import net.byteflux.libby.logging.adapters.LogAdapter;

/**
 * A runtime dependency wrapper with support of transitive dependencies.
 */
public class TransitiveLibraryManager extends LibraryManager {

    /**
     * Delegate {@link LibraryManager}
     */
    private final LibraryManager delegate;

    /**
     * Creates a new {@link TransitiveLibraryManager} simultaneously defining delegate.
     */
    private TransitiveLibraryManager(LogAdapter adapter, Path dataDirectory, String directoryName, LibraryManager delegate) {
        super(adapter, dataDirectory, directoryName);
        this.delegate = delegate;
    }

    /**
     * Wraps a {@link LibraryManager}, adds transitive dependencies support.
     */
    public static TransitiveLibraryManager wrap(LibraryManager delegate) {
        LogAdapter logAdapter = new LoggerLogAdapter(delegate.logger);
        Path dataDirectory = delegate.saveDirectory.getParent();
        String directoryName = delegate.saveDirectory.getFileName().toString();
        return new TransitiveLibraryManager(logAdapter, dataDirectory, directoryName, delegate);
    }

    /**
     * Loads a provided {@link Library}, and transitive dependencies.
     * <p>
     * Dependencies with {@code compile} scope would be loaded as "transitive dependencies"
     * <p>
     * If the provided library has any relocations, repositories, they will be applied to
     * transitive dependencies.
     *
     * @param library           the library to load
     * @param excludedLibraries transitive dependencies that should be skipped
     * @return All loaded transitive dependencies
     * @see #loadLibrary(Library)
     */
    public Collection<Library> loadLibraryTransitively(Library library, ExcludedLibrary... excludedLibraries) {
        loadLibrary(library);

        Collection<ExcludedLibrary> excludedLibraryList = Arrays.asList(excludedLibraries);
        RemoteRepository[] repositories = Stream.of(getRepositories(), library.getRepositories())
                .flatMap(Collection::stream)
                .map(TransitiveDependencyHelper::newDefaultRepository)
                .toArray(RemoteRepository[]::new);

        try {
            return TransitiveDependencyHelper.findCompileDependencies(library.getGroupId(), library.getArtifactId(), library.getVersion(), repositories)
                    .stream()
                    .map(artifact -> adaptArtifact(library, artifact))
                    .peek(transitiveLibrary -> {
                        if (excludedLibraryList.stream().noneMatch(excludedLibrary -> excludedLibrary.similar(transitiveLibrary)))
                            loadLibrary(transitiveLibrary);
                    })
                    .collect(Collectors.toList());
        } catch (DependencyResolutionException e) {
            logger.error("Cannot resolve transitive dependencies of library. Details: " + library, e);
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    /**
     * Adapts an {@link Artifact} into {@link Library}.
     */
    private Library adaptArtifact(Library parent, Artifact artifact) {
        Library.Builder libraryBuilder = Library.builder()
                .groupId(artifact.getGroupId())
                .artifactId(artifact.getArtifactId())
                .version(artifact.getVersion());
        parent.getRelocations().forEach(libraryBuilder::relocate);
        parent.getRepositories().forEach(libraryBuilder::repository);
        return libraryBuilder.build();
    }

    /**
     * Delegates {@code delegate#addToClasspath}
     */
    @Override
    protected void addToClasspath(Path file) {
        delegate.addToClasspath(file);
    }

}
