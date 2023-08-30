package net.byteflux.libby.helper;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;

/**
 * A supplier-based helper class for providing compile scope transitive dependencies.
 *
 * @see <a href=https://github.com/apache/maven-resolver>maven-resolver</a>.
 */
public class TransitiveDependencyHelper {

    /**
     *
     */
    private final RepositorySystem repositorySystem = newRepositorySystem();
    private final RepositorySystemSession repositorySystemSession;
    private final Path saveDirectory;

    public TransitiveDependencyHelper(Path saveDirectory) {
        this.saveDirectory = saveDirectory;
        this.repositorySystemSession = newRepositorySystemSession(repositorySystem);
    }

    public Collection<Artifact> findCompileDependencies(String groupId, String artifactId, String version,
                                                               RemoteRepository... repositories) throws DependencyResolutionException {
        Artifact artifact = new DefaultArtifact(groupId, artifactId, null, "jar", version);
        List<RemoteRepository> repositoryList = Arrays.asList(repositories);

        CollectRequest collectRequest = new CollectRequest(new Dependency(artifact, JavaScopes.COMPILE), repositoryList);
        DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE));

        DependencyResult dependencyResult = repositorySystem.resolveDependencies(repositorySystemSession, dependencyRequest);

        return dependencyResult.getArtifactResults().stream().filter(ArtifactResult::isResolved).map(ArtifactResult::getArtifact).collect(Collectors.toList());
    }

    public Collection<Artifact> findCompileDependencies(String groupId, String artifactId, String version) throws DependencyResolutionException {
        return findCompileDependencies(groupId, artifactId, version, newDefaultRepository("https://repo1.maven.org/maven2/"));
    }

    public static RemoteRepository newDefaultRepository(String url) {
        return new RemoteRepository.Builder(url, "default", url).build();
    }

    private RepositorySystemSession newRepositorySystemSession(RepositorySystem system) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        LocalRepository localRepo = new LocalRepository(saveDirectory.toAbsolutePath().toFile());
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));

        return session;
    }

    private RepositorySystem newRepositorySystem() {
        return new RepositorySystemSupplier().get();
    }

}
