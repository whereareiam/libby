package com.alessiodp.libby.relocation;

import com.alessiodp.libby.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.stream.Collectors;

import static com.alessiodp.libby.Util.replaceWithDots;
import static java.util.Objects.requireNonNull;

/**
 * Relocations are used to describe a search and replace pattern for renaming
 * packages in a library jar for the purpose of preventing namespace conflicts
 * with other java applications that bundle their own version of the same library.
 */
public class Relocation {
    /**
     * Search pattern
     */
    @NotNull
    private final String pattern;

    /**
     * Replacement pattern
     */
    @NotNull
    private final String relocatedPattern;

    /**
     * Classes and resources to include
     */
    @NotNull
    private final Collection<String> includes;

    /**
     * Classes and resources to exclude
     */
    @NotNull
    private final Collection<String> excludes;

    /**
     * Creates a new relocation.
     *
     * @param pattern          search pattern
     * @param relocatedPattern replacement pattern
     * @param includes         classes and resources to include
     * @param excludes         classes and resources to exclude
     */
    public Relocation(@NotNull String pattern, @NotNull String relocatedPattern, @Nullable Collection<String> includes, @Nullable Collection<String> excludes) {
        this.pattern = replaceWithDots(requireNonNull(pattern, "pattern"));
        this.relocatedPattern = replaceWithDots(requireNonNull(relocatedPattern, "relocatedPattern"));
        this.includes = includes != null ? Collections.unmodifiableSet(includes.stream()
                .map(Util::replaceWithDots)
                .collect(Collectors.toSet())
        ) : Collections.emptySet();
        this.excludes = excludes != null ? Collections.unmodifiableSet(excludes.stream()
                .map(Util::replaceWithDots)
                .collect(Collectors.toSet())
        ) : Collections.emptySet();
    }

    /**
     * Creates a new relocation with empty includes and excludes.
     *
     * @param pattern          search pattern
     * @param relocatedPattern replacement pattern
     */
    public Relocation(@NotNull String pattern, @NotNull String relocatedPattern) {
        this(pattern, relocatedPattern, null, null);
    }

    /**
     * Gets the search pattern.
     *
     * @return pattern to search
     */
    @NotNull
    public String getPattern() {
        return pattern;
    }

    /**
     * Gets the replacement pattern.
     *
     * @return pattern to replace with
     */
    @NotNull
    public String getRelocatedPattern() {
        return relocatedPattern;
    }

    /**
     * Gets included classes and resources.
     *
     * @return classes and resources to include
     */
    @NotNull
    public Collection<String> getIncludes() {
        return includes;
    }

    /**
     * Gets excluded classes and resources.
     *
     * @return classes and resources to exclude
     */
    @NotNull
    public Collection<String> getExcludes() {
        return excludes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Relocation that = (Relocation) o;

        if (!pattern.equals(that.pattern)) return false;
        if (!relocatedPattern.equals(that.relocatedPattern)) return false;
        if (!includes.equals(that.includes)) return false;
        return excludes.equals(that.excludes);
    }

    @Override
    public int hashCode() {
        int result = pattern.hashCode();
        result = 31 * result + relocatedPattern.hashCode();
        result = 31 * result + includes.hashCode();
        result = 31 * result + excludes.hashCode();
        return result;
    }

    /**
     * Creates a new relocation builder.
     *
     * @return new relocation builder
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Provides an alternative method of creating a {@link Relocation}. This
     * builder may be more intuitive for configuring relocations that also have
     * any includes or excludes.
     */
    public static class Builder {
        /**
         * Search pattern
         */
        private String pattern;

        /**
         * Replacement pattern
         */
        private String relocatedPattern;

        /**
         * Classes and resources to include
         */
        private final Collection<String> includes = new LinkedList<>();

        /**
         * Classes and resources to exclude
         */
        private final Collection<String> excludes = new LinkedList<>();

        /**
         * Sets the search pattern.
         *
         * @param pattern pattern to search
         * @return this builder
         */
        @NotNull
        public Builder pattern(@NotNull String pattern) {
            this.pattern = requireNonNull(pattern, "pattern");
            return this;
        }

        /**
         * Sets the replacement pattern.
         *
         * @param relocatedPattern pattern to replace with
         * @return this builder
         */
        @NotNull
        public Builder relocatedPattern(@NotNull String relocatedPattern) {
            this.relocatedPattern = requireNonNull(relocatedPattern, "relocatedPattern");
            return this;
        }

        /**
         * Adds a class or resource to be included.
         *
         * @param include class or resource to include
         * @return this builder
         */
        @NotNull
        public Builder include(@NotNull String include) {
            includes.add(requireNonNull(include, "include"));
            return this;
        }

        /**
         * Adds a class or resource to be excluded.
         *
         * @param exclude class or resource to exclude
         * @return this builder
         */
        @NotNull
        public Builder exclude(@NotNull String exclude) {
            excludes.add(requireNonNull(exclude, "exclude"));
            return this;
        }

        /**
         * Creates a new relocation using this builder's configuration.
         *
         * @return new relocation
         */
        @NotNull
        public Relocation build() {
            return new Relocation(pattern, relocatedPattern, includes, excludes);
        }
    }
}
