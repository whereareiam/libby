package com.alessiodp.libby.transitive;

import org.jetbrains.annotations.NotNull;

import static com.alessiodp.libby.Util.replaceWithDots;
import static java.util.Objects.requireNonNull;

/**
 * Represents a dependency to exclude during transitive dependency resolution for a library.
 */
public class ExcludedDependency {
    /**
     * Maven group ID
     */
    @NotNull
    private final String groupId;

    /**
     * Maven artifact ID
     */
    @NotNull
    private final String artifactId;

    /**
     * Creates a new {@link ExcludedDependency}
     *
     * @param groupId    Maven group ID
     * @param artifactId Maven artifact ID
     */
    public ExcludedDependency(@NotNull String groupId, @NotNull String artifactId) {
        this.groupId = replaceWithDots(requireNonNull(groupId, "groupId"));
        this.artifactId = replaceWithDots(requireNonNull(artifactId, "artifactId"));
    }

    /**
     * Gets the Maven group ID for this excluded dependency
     *
     * @return Maven group ID
     */
    @NotNull
    public String getGroupId() {
        return groupId;
    }

    /**
     * Gets the Maven artifact ID for this excluded dependency
     *
     * @return Maven artifact ID
     */
    @NotNull
    public String getArtifactId() {
        return artifactId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExcludedDependency that = (ExcludedDependency) o;

        if (!groupId.equals(that.groupId)) return false;
        return artifactId.equals(that.artifactId);
    }

    @Override
    public int hashCode() {
        int result = groupId.hashCode();
        result = 31 * result + artifactId.hashCode();
        return result;
    }
}
