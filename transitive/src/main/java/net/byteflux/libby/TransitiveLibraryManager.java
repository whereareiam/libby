package net.byteflux.libby;

import net.byteflux.libby.helper.DependencyTreeHelper;
import net.byteflux.libby.logging.adapters.LogAdapter;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.DependencyResolutionException;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

import static net.byteflux.libby.helper.DependencyTreeHelper.findCompileDependencies;

public class TransitiveLibraryManager extends LibraryManager {
    private final LibraryManager delegate;

    private TransitiveLibraryManager(LogAdapter adapter, Path dataDirectory, String directoryName, LibraryManager delegate) {
        super(adapter, dataDirectory, directoryName);
        this.delegate = delegate;
    }

    public static TransitiveLibraryManager wrap(LibraryManager delegate) {
        LogAdapter logAdapter = new LoggerLogAdapter(delegate.logger);
        Path dataDirectory = delegate.saveDirectory.getParent();
        String directoryName = delegate.saveDirectory.getFileName().toString();
        return new TransitiveLibraryManager(logAdapter, dataDirectory, directoryName, delegate);
    }

    public void loadLibraryTransitively(Library library, ExcludedLibrary... excludedLibraries) {
        loadLibrary(library);

        Collection<ExcludedLibrary> excludedLibraryList = Arrays.asList(excludedLibraries);
        RemoteRepository[] repositories = Stream.of(getRepositories(), library.getRepositories())
                                                .flatMap(Collection::stream)
                                                .map(DependencyTreeHelper::newDefaultRepository)
                                                .toArray(RemoteRepository[]::new);

        try {
            findCompileDependencies(library.getGroupId(), library.getArtifactId(), library.getVersion(), repositories).forEach(transitiveArtifact -> {
                Library transitiveLibrary = adaptArtifact(library, transitiveArtifact);
                if (excludedLibraryList.stream().noneMatch(excludedLibrary -> excludedLibrary.similar(transitiveLibrary)))
                    loadLibrary(transitiveLibrary);
            });
        } catch (DependencyResolutionException | ArtifactDescriptorException e) {
            logger.error("Cannot resolve transitive dependencies of library. Details: " + library, e);
            e.printStackTrace();
        }
    }

    private Library adaptArtifact(Library parent, Artifact artifact) {
        Library.Builder libraryBuilder = Library.builder()
                                                .groupId(artifact.getGroupId())
                                                .artifactId(artifact.getArtifactId())
                                                .version(artifact.getVersion());
        parent.getRelocations().forEach(libraryBuilder::relocate);
        parent.getRepositories().forEach(libraryBuilder::repository);
        return libraryBuilder.build();
    }

    @Override
    protected void addToClasspath(Path file) {
        delegate.addToClasspath(file);
    }
}
