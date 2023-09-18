package com.alessiodp.libby.transitive;

import com.alessiodp.libby.Library;
import com.alessiodp.libby.LibraryManagerMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class TransitiveDownloadingTest {

    private static final Library MAVEN_RESOLVER_SUPPLIER = Library.builder()
            .groupId("org{}apache{}maven{}resolver")
            .artifactId("maven-resolver-supplier")
            .version("1.9.15")
            .resolveTransitiveDependencies(true)
            .build();

    private LibraryManagerMock libraryManager;

    @BeforeEach
    void setUp() throws Exception {
        libraryManager = new LibraryManagerMock();
        libraryManager.addMavenCentral();
    }

    @AfterEach
    void tearDown() {
        libraryManager = null;
    }

    @Test
    void transitiveLoad() {
        libraryManager.loadLibrary(MAVEN_RESOLVER_SUPPLIER);

        // This compares the libraries required by maven-resolver-supplier with the ones declared in TransitiveLibraryResolutionDependency
        Set<String> expectedDependencies = Arrays.stream(TransitiveLibraryResolutionDependency.values())
                .filter(dep -> dep != TransitiveLibraryResolutionDependency.JAVAX_INJECT) // Skip javax.inject since it is excluded by maven-resolver-supplier
                .map(TransitiveLibraryResolutionDependency::toLibrary)
                .map(Library::getPath)
                .map(path -> libraryManager.getSaveDirectory().resolve(path).toAbsolutePath().toString())
                .collect(Collectors.toSet());

        Set<String> loaded = libraryManager.getLoaded();
        assertEquals(expectedDependencies, loaded);
    }
}
