package com.alessiodp.libby.transitive;

import static java.util.Objects.requireNonNull;

/**
 * Represents a dependency to exclude during transitive dependency resolution for a library.
 */
public class ExcludedDependency {
    /**
     * Maven group ID
     */
    private final String groupId;

    /**
     * Maven artifact ID
     */
    private final String artifactId;

    /**
     * Creates a new {@link ExcludedDependency}
     *
     * @param groupId    Maven group ID
     * @param artifactId Maven artifact ID
     */
    public ExcludedDependency(String groupId, String artifactId) {
        this.groupId = requireNonNull(groupId, "groupId").replace("{}", ".");
        this.artifactId = requireNonNull(artifactId, "artifactId");
    }

    /**
     * Gets the Maven group ID for this excluded dependency
     *
     * @return Maven group ID
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * Gets the Maven artifact ID for this excluded dependency
     *
     * @return Maven artifact ID
     */
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
