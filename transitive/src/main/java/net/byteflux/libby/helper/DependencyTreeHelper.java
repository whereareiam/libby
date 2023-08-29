package net.byteflux.libby.helper;

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
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.ScopeDependencyFilter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DependencyTreeHelper {
    private static final RepositorySystem REPOSITORY_SYSTEM = newRepositorySystem();
    private static final RepositorySystemSession REPOSITORY_SYSTEM_SESSION = newRepositorySystemSession(REPOSITORY_SYSTEM);

    private DependencyTreeHelper() {
    }

    public static Collection<Artifact> findCompileDependencies(String groupId, String artifactId,
                                                               String version, RemoteRepository... repositories) throws DependencyResolutionException, ArtifactDescriptorException {
        Artifact artifact = new DefaultArtifact(groupId, artifactId, null, "jar", version);
        List<RemoteRepository> repositoryList = Arrays.asList(repositories);

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(artifact, null));
        collectRequest.setRepositories(repositoryList);

        ArtifactDescriptorRequest descriptorRequest = new ArtifactDescriptorRequest();
        descriptorRequest.setArtifact(artifact);
        descriptorRequest.setRepositories(repositoryList);
        ArtifactDescriptorResult descriptorResult = REPOSITORY_SYSTEM.readArtifactDescriptor(REPOSITORY_SYSTEM_SESSION, descriptorRequest);
        collectRequest.setDependencies(descriptorResult.getDependencies());

        DependencyRequest dependencyRequest = new DependencyRequest();
        dependencyRequest.setCollectRequest(collectRequest);
        dependencyRequest.setFilter(new ScopeDependencyFilter(Collections.singleton(JavaScopes.COMPILE), null));
        DependencyResult dependencyResult = REPOSITORY_SYSTEM.resolveDependencies(REPOSITORY_SYSTEM_SESSION, dependencyRequest);

        return dependencyResult.getArtifactResults().stream().filter(ArtifactResult::isResolved).map(ArtifactResult::getArtifact).collect(Collectors.toList());
    }

    public static Collection<Artifact> findCompileDependencies(String groupId, String artifactId,
                                                               String version) throws DependencyResolutionException, ArtifactDescriptorException {
        return findCompileDependencies(groupId, artifactId, version, newDefaultRepository("central", "https://repo1.maven.org/maven2/"));
    }

    public static RemoteRepository newDefaultRepository(String id, String url) {
        return new RemoteRepository.Builder(id, "default", url).build();
    }

    public static RemoteRepository newDefaultRepository(String url) {
        return new RemoteRepository.Builder(null, "default", url).build();
    }

    private static RepositorySystemSession newRepositorySystemSession(RepositorySystem system) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        LocalRepository localRepo = new LocalRepository("target/local-repo");
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));

        return session;
    }

    private static RepositorySystem newRepositorySystem() {
        return new RepositorySystemSupplier().get();
    }
}
