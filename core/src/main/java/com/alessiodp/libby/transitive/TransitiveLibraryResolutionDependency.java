package com.alessiodp.libby.transitive;

import com.alessiodp.libby.Library;
import com.alessiodp.libby.Repositories;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the libraries required for Maven transitive dependency resolution and related operations.
 * <p>
 * This class bundles Maven Resolver Supplier, Maven Resolver Provider and their transitive dependencies.
 */
enum TransitiveLibraryResolutionDependency {
    MAVEN_RESOLVER_SUPPLIER("org{}apache{}maven{}resolver", "maven-resolver-supplier", "1.9.15", "BNncFRDRqBBHWixE0DYfFydh7h9bc3mhC/MBr5WD448"),
    MAVEN_RESOLVER_API("org{}apache{}maven{}resolver", "maven-resolver-api", "1.9.15", "1Ugp1gooJ7uXYxK9u/YnlR5hMpJ2LuFGFAxRqn3OYL4"),
    MAVEN_RESOLVER_UTIL("org{}apache{}maven{}resolver", "maven-resolver-util", "1.9.15", "z9Gnzg0gpWHtZrI18lpvwa5A/f8Zr6k1GrCpZNTlVm8"),
    MAVEN_RESOLVER_SPI("org{}apache{}maven{}resolver", "maven-resolver-spi", "1.9.15", "V2xyiu6a+awHPayBvrdsgZ4zGMIzeRL0Xr7xmD5pv6U"),
    MAVEN_RESOLVER_NAMED_LOCKS("org{}apache{}maven{}resolver", "maven-resolver-named-locks", "1.9.15", "/8bnO65ieplawVG0wI2v/K2cOltLHGo/8wL53dpMGB4"),
    MAVEN_RESOLVER_IMPL("org{}apache{}maven{}resolver", "maven-resolver-impl", "1.9.15", "APYerOvm4uPbNrGQ/72JvfZJXTugrtmCI2nYYx0Ctr4"),
    MAVEN_RESOLVER_CONNECTOR_BASIC("org{}apache{}maven{}resolver", "maven-resolver-connector-basic", "1.9.15", "NrdEncaUGNjstCH8BqckU5gNzFbVfbPDQ7FvRMeXcmw"),
    MAVEN_RESOLVER_TRANSPORT_FILE("org{}apache{}maven{}resolver", "maven-resolver-transport-file", "1.9.15", "JAJAXaKiQyTVsVAJyc9l/CKjsNZirxUzgxw7ix7Fzus"),
    MAVEN_RESOLVER_TRANSPORT_HTTP("org{}apache{}maven{}resolver", "maven-resolver-transport-http", "1.9.15", "kDEw+zmNvtsTZM3tdxe6OqRAhKHkAxYM+xfn7L3E5Fk"),
    HTTPCLIENT("org{}apache{}httpcomponents", "httpclient", "4.5.14", "yLx+HFGm1M5y9A0uu6vxxLaL/nbnMhBLBDgbSTR46dY"),
    HTTPCORE("org{}apache{}httpcomponents", "httpcore", "4.4.16", "bJs90UKgncRo4jrTmq1vdaDyuFElEERp8CblKkdORk8"),
    COMMONS_CODEC("commons-codec", "commons-codec", "1.16.0", "VllfsgsLhbyR0NUD2tULt/G5r8Du1d/6bLslkpAASE0"),
    JCL_OVER_SLF4J("org{}slf4j", "jcl-over-slf4j", "1.7.36", "q1fKj9IjdywXNl0SH1npTsvwrlnQjAOjy1uBBxwBkZU"),
    MAVEN_MODEL_BUILDER("org{}apache{}maven", "maven-model-builder", "3.9.4", "gVsYhtHsjh2LV7YeasgyNb4/amAVnI7lpvTpH1E/SX8"),
    PLEXUS_INTERPOLATION("org{}codehaus{}plexus", "plexus-interpolation", "1.26", "s7VBLOF4iRA+pWS838+fs9+lQDRP/qxrU4pzydcYJmI"),
    MAVEN_ARTIFACT("org{}apache{}maven", "maven-artifact", "3.9.4", "fdNS/Z+P+GodCn2J5iidjTzTRqybIU7YWGjVhb4Fq3g"),
    COMMONS_LANG3("org{}apache{}commons", "commons-lang3", "3.12.0", "2RnZBEhsA3+NGTQS2gyS4iqfokIwudZ6V4VcXDHH6U4"),
    MAVEN_BUILDER_SUPPORT("org{}apache{}maven", "maven-builder-support", "3.9.4", "hpRvsGyyBVFVObk7AFw7uSi6PQ0UCvaAecxMyBUSUJY"),
    SLF4J_API("org{}slf4j", "slf4j-api", "1.7.36", "0+9XXj5JeWeNwBvx3M5RAhSTtNEft/G+itmCh3wWocA"),
    SLF4J_NOP("org{}slf4j", "slf4j-nop", "1.7.36", "whSViweBbLRBKzDHvb1DCP/ca6KoN2e486kinL2SdNY="),
    MAVEN_RESOLVER_PROVIDER("org{}apache{}maven", "maven-resolver-provider", "3.9.4", "upvxL9L6RXgCNbthu/fkWTxg10HwCOCJuZkp6Ma1iqk"),
    MAVEN_MODEL("org{}apache{}maven", "maven-model", "3.9.4", "eTHcjdqHju9GmI2f0HxK3jyRiS1/syCMdCy5RXE1uxU"),
    MAVEN_REPOSITORY_METADATA("org{}apache{}maven", "maven-repository-metadata", "3.9.4", "HVwzk6o5+AeDf7BY14rI0KKHrfGfS6atUHA1Os9hsIE"),
    PLEXUS_UTILS("org{}codehaus{}plexus", "plexus-utils", "3.5.1", "huAlXUyHnGG0gz7X8TEk6LtnnfR967EnMm59t91JoHs"),
    JAVAX_INJECT("javax{}inject", "javax{}inject", "1", "kcdwRKUMSBY2wy2Rb9ickRinIZU5BFLIEGUID5V95/8");

    private final String groupId, artifactId, version, checksum;

    TransitiveLibraryResolutionDependency(@NotNull String groupId, @NotNull String artifactId, @NotNull String version, @NotNull String checksum) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.checksum = checksum;
    }

    @NotNull
    public Library toLibrary() {
        return Library.builder()
                .groupId(groupId)
                .artifactId(artifactId)
                .version(version)
                .checksumFromBase64(checksum)
                .repository(Repositories.MAVEN_CENTRAL)
                // Relocate all packages used in Libby to avoid conflicts
                .relocate("org{}eclipse{}aether{}util", "org.eclipse.aether.util") // maven-resolver-util
                .relocate("org{}eclipse{}aether", "org.eclipse.aether") // maven-resolver-api
                .relocate("org{}apache{}maven{}repository{}internal", "org.apache.maven.repository.internal") // maven-resolver-provider

                .build();
    }
}
