import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    implementation("org.apache.maven.resolver:maven-resolver-impl:1.9.18")
    implementation("org.apache.maven.resolver:maven-resolver-supplier:1.9.18")
}

tasks.withType<ShadowJar> {
    archiveClassifier.set("")
    minimize()
}

tasks.jar {
    finalizedBy("shadowJar")
}
