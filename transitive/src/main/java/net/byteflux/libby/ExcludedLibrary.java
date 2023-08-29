package net.byteflux.libby;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class ExcludedLibrary {
    private final String groupId;
    private final String artifactId;

    private ExcludedLibrary(String groupId, String artifactId) {
        this.groupId = requireNonNull(groupId, "groupId").replace("{}", ".");
        this.artifactId = requireNonNull(artifactId, "artifactId");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ExcludedLibrary of(String groupId, String artifactId) {
        return builder().groupId(groupId).artifactId(artifactId).build();
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public boolean similar(Library library) {
        return Objects.equals(groupId, library.getGroupId()) && Objects.equals(artifactId, library.getArtifactId());
    }

    public static class Builder {
        private String groupId;
        private String artifactId;

        public Builder groupId(String groupId) {
            this.groupId = requireNonNull(groupId, "groupId");
            return this;
        }

        public Builder artifactId(String artifactId) {
            this.artifactId = artifactId;
            return this;
        }

        public ExcludedLibrary build() {
            return new ExcludedLibrary(groupId, artifactId);
        }
    }
}
