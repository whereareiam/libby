package com.alessiodp.libby;

import com.alessiodp.libby.configuration.Configuration;
import com.alessiodp.libby.configuration.ConfigurationException;
import com.alessiodp.libby.configuration.ConfigurationFetcher;
import com.alessiodp.libby.relocation.Relocation;
import com.alessiodp.libby.transitive.ExcludedDependency;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.alessiodp.libby.Util.replaceWithDots;
import static org.junit.jupiter.api.Assertions.*;

public class ConfigurationFetcherTest {

    private LibraryManagerMock libraryManager;
    private ConfigurationFetcher configurationFetcher;

    @BeforeEach
    public void setUp() throws Exception {
        libraryManager = new LibraryManagerMock();
        configurationFetcher = new ConfigurationFetcher(libraryManager);
    }

    @AfterEach
    public void tearDown() {
        libraryManager = null;
        configurationFetcher = null;
    }

    @Test
    public void testFromFile() throws Exception {
        InputStream fileInputStream = getClass().getClassLoader().getResourceAsStream("libby.json");
        assertNotNull(fileInputStream);
        Configuration config = configurationFetcher.readJsonFile(fileInputStream);

        Integer version = config.getVersion();
        assertNotNull(version);
        assertEquals(0, version);

        Set<String> repositories = config.getRepositories();
        assertEquals(2, repositories.size());
        assertTrue(repositories.contains("repo1"));
        assertTrue(repositories.contains("repo2"));

        Set<Relocation> globalRelocations = config.getGlobalRelocations();
        assertEquals(1, globalRelocations.size());
        Relocation globalRelocation = new Relocation("fake{}library{}1", "relocated{}fake{}library{}1");
        assertTrue(globalRelocations.contains(globalRelocation));

        List<Library> libraries = config.getLibraries();
        assertEquals(2, libraries.size());
        assertTrue(libraries.stream().anyMatch(l -> l.getGroupId().equals(replaceWithDots("fake{}library{}1"))
                && l.getArtifactId().equals("library-1")
                && l.getVersion().equals("1.0.0")
                && !l.hasClassifier()
                && l.isIsolatedLoad()
                && l.getLoaderId() != null && l.getLoaderId().equals("isolatedLoader1")
                && l.resolveTransitiveDependencies()
                && compareCollections(
                        l.getExcludedTransitiveDependencies(),
                        new ExcludedDependency("excludedDep1{}groupId", "excludedDep1{}artifactId"),
                        new ExcludedDependency("excludedDep2{}groupId", "excludedDep2{}artifactId")
                   )
                && compareCollections(
                        l.getRepositories(),
                        "libraryRepo1/", // Add a '/' at the end since it is added by the Library builder
                        "libraryRepo2/"
                   )
                && compareCollections(
                        l.getRelocations(),
                        globalRelocation // Global
                   )
        ));
        assertTrue(libraries.stream().anyMatch(l -> l.getGroupId().equals(replaceWithDots("fake{}library{}2"))
                && l.getArtifactId().equals("library-2")
                && l.getVersion().equals("1.0.0")
                && l.hasClassifier()
                && "aClassifier".equals(l.getClassifier())
                && l.getRepositories().isEmpty()
                && l.getExcludedTransitiveDependencies().isEmpty()
                && compareCollections(
                        l.getRelocations(),
                        globalRelocation, // Global
                        new Relocation("fake{}library{}2", "relocated{}fake{}library{}2"), // Local
                        new Relocation("fake{}library{}3", "relocated{}fake{}library{}3",  // Local
                                Arrays.asList("include{}1", "include{}2"), Arrays.asList("exclude{}1", "exclude{}2")
                        )
                   )
        ));
    }

    @Test
    public void testFails() {
        Exception ex;

        // Version
        assertThrows(ConfigurationException.class, () -> parseAndRead("{\"version\":-1}"));

        // Relocations
        ex = assertThrows(ConfigurationException.class, () -> parseAndRead("{\"relocations\":[{}]}"));
        assertTrue(ex.getMessage().contains("pattern property"));
        ex = assertThrows(ConfigurationException.class, () -> parseAndRead("{\"relocations\":[{\"pattern\":\"\"}]}"));
        assertTrue(ex.getMessage().contains("relocatedPattern property"));

        // Libraries
        ex = assertThrows(ConfigurationException.class, () -> parseAndRead("{\"libraries\":[{}]}"));
        assertTrue(ex.getMessage().contains("groupId property"));
        ex = assertThrows(ConfigurationException.class, () -> parseAndRead("{\"libraries\":[{\"groupId\":\"\"}]}"));
        assertTrue(ex.getMessage().contains("artifactId property"));
        ex = assertThrows(ConfigurationException.class, () -> parseAndRead("{\"libraries\":[{\"groupId\":\"\",\"artifactId\":\"\"}]}"));
        assertTrue(ex.getMessage().contains("version property"));

        // Invalid checksum
        ex = assertThrows(ConfigurationException.class, () -> parseAndRead("{\"libraries\":[{\"groupId\":\"\",\"artifactId\":\"\",\"version\":\"\",\"checksumFromBase64\":\"invalid-checksum\"}]}"));
        assertTrue(ex.getMessage().contains("valid base64"));
    }

    private void parseAndRead(String json) {
        configurationFetcher.readJsonFile(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
    }

    @SafeVarargs
    private final <T> boolean compareCollections(Collection<T> collection, T... expectedContents) {
        if (expectedContents.length != collection.size()) {
            return false;
        }
        Set<T> coll = new HashSet<>(collection);
        for (T expected : expectedContents) {
            if (!coll.contains(expected)) {
                return false;
            }
        }
        return true;
    }
}
