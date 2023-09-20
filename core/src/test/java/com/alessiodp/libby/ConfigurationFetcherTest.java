package com.alessiodp.libby;

import com.alessiodp.libby.relocation.Relocation;
import com.alessiodp.libby.transitive.ExcludedDependency;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConfigurationFetcherTest {
    private final ConfigurationFetcher configurationFetcher = new ConfigurationFetcher();

    @Test
    public void testFromFile() throws JsonParserException {
        JsonObject config = JsonParser.object().from(getClass().getClassLoader().getResourceAsStream("libby.json"));

        assertEquals(0, configurationFetcher.fetchVersion(config));

        Set<String> repositories = configurationFetcher.fetchRepositories(config);
        assertEquals(2, repositories.size());
        assertTrue(repositories.contains("repo1"));
        assertTrue(repositories.contains("repo2"));

        List<Relocation> relocations = configurationFetcher.fetchRelocations(config);
        assertEquals(1, relocations.size());
        assertTrue(relocations.stream().anyMatch(r -> r.getPattern().equals(replaceWithDots("fake{}library{}1"))
                && r.getRelocatedPattern().equals(replaceWithDots("relocated{}fake{}library{}1"))));

        List<Library> libraries = configurationFetcher.fetchLibraries(config, relocations);
        assertEquals(2, libraries.size());
        assertTrue(libraries.stream().anyMatch(l -> l.getGroupId().equals(replaceWithDots("fake{}library{}1"))
                && l.getArtifactId().equals("library-1")
                && l.getVersion().equals("1.0.0")
                && !l.hasClassifier()
                && l.isIsolatedLoad()
                && l.getLoaderId().equals("isolatedLoader1")
                && l.resolveTransitiveDependencies()
                && compareCollections(
                        l.getExcludedTransitiveDependencies(),
                        new ExcludedDependency(replaceWithDots("excludedDep1{}groupId"), replaceWithDots("excludedDep1{}artifactId")),
                        new ExcludedDependency(replaceWithDots("excludedDep2{}groupId"), replaceWithDots("excludedDep2{}artifactId"))
                   )
                && compareCollections(
                        l.getRepositories(),
                        "libraryRepo1/", // Add a '/' at the end since it is added by the Library builder
                        "libraryRepo2/"
                   )
                && l.getRelocations().size() == 1)); // 1 global relocation
        assertTrue(libraries.stream().anyMatch(l -> l.getGroupId().equals(replaceWithDots("fake{}library{}2"))
                && l.getArtifactId().equals("library-2")
                && l.getVersion().equals("1.0.0")
                && l.hasClassifier()
                && "aClassifier".equals(l.getClassifier())
                && l.getRepositories().isEmpty()
                && l.getExcludedTransitiveDependencies().isEmpty()
                && l.getRelocations().size() == 2)); // 1 global relocation + 1 local
    }

    @Test
    public void testFails() {
        Exception ex;
        JsonObject config = JsonObject.builder()
                .value("version", 1)
                .array("relocations")
                    .object() // Invalid relocation
                    .end()
                .end()
                .array("libraries")
                    .object() // Invalid library
                    .end()
                .end()
                .done();

        // Version
        assertThrows(IllegalArgumentException.class, () -> configurationFetcher.fetchVersion(config));

        // Relocations
        ex = assertThrows(IllegalArgumentException.class, () -> configurationFetcher.fetchRelocations(config));
        assertTrue(ex.getMessage().contains("pattern property"));
        config.getArray("relocations").getObject(0).put("pattern", ""); // Add pattern field
        ex = assertThrows(IllegalArgumentException.class, () -> configurationFetcher.fetchRelocations(config));
        assertTrue(ex.getMessage().contains("relocatedPattern property"));

        // Libraries
        ex = assertThrows(IllegalArgumentException.class, () -> configurationFetcher.fetchLibraries(config, Collections.emptyList()));
        assertTrue(ex.getMessage().contains("group property"));
        config.getArray("libraries").getObject(0).put("group", ""); // Add group field
        ex = assertThrows(IllegalArgumentException.class, () -> configurationFetcher.fetchLibraries(config, Collections.emptyList()));
        assertTrue(ex.getMessage().contains("name property"));
        config.getArray("libraries").getObject(0).put("name", ""); // Add name field
        ex = assertThrows(IllegalArgumentException.class, () -> configurationFetcher.fetchLibraries(config, Collections.emptyList()));
        assertTrue(ex.getMessage().contains("version property"));
        config.getArray("libraries").getObject(0).put("version", ""); // Add version field

        // Invalid checksum
        config.getArray("libraries").getObject(0).put("checksum", "invalid-checksum");
        ex = assertThrows(IllegalArgumentException.class, () -> configurationFetcher.fetchLibraries(config, Collections.emptyList()));
        assertTrue(ex.getMessage().contains("valid base64"));
    }

    private String replaceWithDots(String str) {
        return str.replace("{}", ".");
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
