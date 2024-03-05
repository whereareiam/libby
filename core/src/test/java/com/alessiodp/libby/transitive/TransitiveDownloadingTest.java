package com.alessiodp.libby.transitive;

import com.alessiodp.libby.Library;
import com.alessiodp.libby.LibraryManagerMock;
import com.alessiodp.libby.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class TransitiveDownloadingTest {

    private static final Library MAVEN_RESOLVER_SUPPLIER = Library.builder()
            .groupId("org{}apache{}maven{}resolver")
            .artifactId("maven-resolver-supplier")
            .version("1.9.15")
            .resolveTransitiveDependencies(true)
            .build();
    private static final Library EXCLUDED_LIBRARY = TransitiveLibraryResolutionDependency.MAVEN_RESOLVER_API.toLibrary();
    private static final Library MAVEN_RESOLVER_SUPPLIER_WITH_EXCLUDED = Library.builder()
            .groupId(MAVEN_RESOLVER_SUPPLIER.getGroupId())
            .artifactId(MAVEN_RESOLVER_SUPPLIER.getArtifactId())
            .version(MAVEN_RESOLVER_SUPPLIER.getVersion())
            .resolveTransitiveDependencies(true)
            .excludeTransitiveDependency(EXCLUDED_LIBRARY.getGroupId(), EXCLUDED_LIBRARY.getArtifactId())
            .build();
    private static final Library BUNGEECORD = Library.builder()
            .groupId("net{}md-5")
            .artifactId("bungeecord-api")
            .version("1.20-R0.2-SNAPSHOT")
            .repository("https://oss.sonatype.org/content/repositories/snapshots")
            .isolatedLoad(true)
            .loaderId("bungeecord")
            .resolveTransitiveDependencies(true)
            .build();

    private LibraryManagerMock libraryManager;

    @BeforeEach
    public void setUp() throws Exception {
        libraryManager = new LibraryManagerMock();
        libraryManager.addMavenCentral();
    }

    @AfterEach
    public void tearDown() {
        libraryManager = null;
    }

    @Test
    public void transitiveLoad() {
        libraryManager.loadLibrary(MAVEN_RESOLVER_SUPPLIER);

        checkDownloadedDependencies();
    }

    @Test
    public void snapshotLibraryTransitiveLoad() throws Exception {
        libraryManager.loadLibrary(BUNGEECORD);

        assertNotNull(libraryManager.getIsolatedClassLoaderById("bungeecord").loadClass("net.md_5.bungee.api.ProxyServer"));
    }

    @Test
    public void transitiveWithExcludedLoad() {
        libraryManager.loadLibrary(MAVEN_RESOLVER_SUPPLIER_WITH_EXCLUDED);

        checkDownloadedDependencies(TransitiveLibraryResolutionDependency.MAVEN_RESOLVER_API);
    }

    /**
     * Compares the libraries required by maven-resolver-supplier with the ones declared in {@link TransitiveLibraryResolutionDependency}.
     *
     * @param excludedDependencies Optionally excluded more dependencies
     */
    private void checkDownloadedDependencies(TransitiveLibraryResolutionDependency... excludedDependencies) {
        Set<TransitiveLibraryResolutionDependency> excluded = new HashSet<>();
        // Always exclude javax.inject since it is excluded by maven-resolver-supplier
        excluded.add(TransitiveLibraryResolutionDependency.JAVAX_INJECT);
        // Always exclude slf4j-nop since it is not included in maven-resolver-supplier
        excluded.add(TransitiveLibraryResolutionDependency.SLF4J_NOP);
        excluded.addAll(Arrays.asList(excludedDependencies));

        List<String> loaded = libraryManager.getLoaded();
        // Assert that the correct amount of libraries has been loaded
        assertEquals(TransitiveLibraryResolutionDependency.values().length - excluded.size(), loaded.size());

        Arrays.stream(TransitiveLibraryResolutionDependency.values())
                .filter(dep -> !excluded.contains(dep))
                .map(TransitiveLibraryResolutionDependency::toLibrary)
                .forEach(dep -> {
                    Path path = libraryManager.getSaveDirectory().resolve(dep.getPath()).toAbsolutePath();
                    assertTrue(loaded.contains(path.toString()));
                    TestUtils.assertCorrectFile(path, dep.getChecksum());
                });
    }
}
