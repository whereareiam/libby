package com.alessiodp.libby;

import com.alessiodp.libby.relocation.Relocation;
import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;

import java.util.HashSet;
import java.util.Set;

/**
 * This class is used to fetch the JSON configuration file. It can be extended to change the fetching behavior.
 */
public class ConfigurationFetcher {
	/**
	 * Fetch the optional version of the JSON file and, if exists, it must match the version of the parser
	 *
	 * @param configuration the root object of the JSON file
	 * @return the fetched version or -1 if not found
	 */
	public int fetchVersion(JsonObject configuration) {
		int version = configuration.getInt("version", -1);
		
		if (version >= 0 && version != 0) {
			throw new IllegalArgumentException("The json file is version " + version + " but this version of libby only supports version 0");
		}
		return version;
	}
	
	/**
	 * Fetch the repositories from the JSON file. It can be omitted from the JSON.
	 * If defined, it must be an array of string representing the repository URLs.
	 *
	 * @param configuration the root object of the JSON file
	 * @return the set of repositories as strings
	 */
	public Set<String> fetchRepositories(JsonObject configuration) {
		Set<String> repos = new HashSet<>();
		JsonArray repositories = configuration.getArray("repositories");
		if (repositories != null) {
			for (int i = 0; i < repositories.size(); i++) {
				repos.add(repositories.getString(i));
			}
		}
		return repos;
	}
	
	/**
	 * Fetch the relocations from the JSON file. It can be omitted from the JSON.
	 * If defined, they must be an array of objects that include the following properties:
	 * <ul>
	 *     <li>pattern: The search pattern</li>
	 *     <li>relocatedPattern: The replacement pattern</li>
	 * </ul>
	 *
	 * @param configuration the root object of the JSON file
	 * @return the set of relocations as Relocation
	 */
	public Set<Relocation> fetchRelocations(JsonObject configuration) {
		Set<Relocation> fetchedRelocations = new HashSet<>();
		JsonArray relocations = configuration.getArray("relocations");
		if (relocations != null) {
			for (int i = 0; i < relocations.size(); i++) {
				JsonObject relocation = relocations.getObject(i);
				
				String pattern = relocation.getString("pattern");
				
				if (pattern == null) {
					throw new IllegalArgumentException("The pattern property is required for all relocations");
				}
				
				String relocatedPattern = relocation.getString("relocatedPattern");
				
				if (relocatedPattern == null) {
					throw new IllegalArgumentException("The relocatedPattern property is required for all relocations");
				}
				
				fetchedRelocations.add(new Relocation(pattern, relocatedPattern));
			}
		}
		return fetchedRelocations;
	}
	
	/**
	 * Fetch the libraries from the JSON file. It can be omitted from the JSON.
	 * If defined, they must be an array of objects that include the following properties:
	 * <ul>
	 *     <li>group: The groupId of the library</li>
	 *     <li>name: The artifactId of the library</li>
	 *     <li>version: The version of the library</li>
	 * </ul>
	 * Optional properties:
	 * <ul>
	 *     <li>checksum: The SHA-256 checksum of the library, must be a base64 encoded string and may only be included if the library is a JAR</li>
	 * </ul>
	 *
	 * @param configuration the root object of the JSON file
	 * @param globalRelocations the set of global relocations to apply to all libraries
	 * @return the set of relocations as Relocation
	 */
	public Set<Library> fetchLibraries(JsonObject configuration, Set<Relocation> globalRelocations) {
		Set<Library> fetchedLibraries = new HashSet<>();
		JsonArray libraries = configuration.getArray("libraries");
		
		if (libraries != null) {
			for (int i = 0; i < libraries.size(); i++) {
				JsonObject library = libraries.getObject(i);
				Library.Builder libraryBuilder = Library.builder();
				
				String groupId = library.getString("group");
				
				if (groupId == null) {
					throw new IllegalArgumentException("The group property is required for all libraries");
				}
				
				String artifactId = library.getString("name");
				
				if (artifactId == null) {
					throw new IllegalArgumentException("The name property is required for all libraries");
				}
				
				String artifactVersion = library.getString("version");
				
				if (artifactVersion == null) {
					throw new IllegalArgumentException("The version property is required for all libraries");
				}
				
				libraryBuilder
						.groupId(groupId)
						.artifactId(artifactId)
						.version(artifactVersion);
				
				String checksum = library.getString("checksum");
				
				if (checksum != null) {
					try {
						libraryBuilder.checksum(checksum);
					} catch (IllegalArgumentException ignored) {
						throw new IllegalArgumentException("The checksum property must be a valid base64 encoded SHA-256 checksum");
					}
				}

				libraryBuilder.isolatedLoad(library.getBoolean("isolatedLoad"));

				String id = library.getString("id");

				if (id != null) {
					libraryBuilder.id(id);
				}

				Set<Relocation> relocations = fetchRelocations(library);
				
				// Apply relocation
				for (Relocation relocation : relocations) {
					libraryBuilder.relocate(relocation);
				}
				
				// Apply global defined relocations
				for (Relocation relocation : globalRelocations) {
					libraryBuilder.relocate(relocation);
				}
				
				fetchedLibraries.add(libraryBuilder.build());
			}
		}
		return fetchedLibraries;
	}
}
