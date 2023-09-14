import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("net.kyori.blossom") version "2.0.1"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    implementation("com.grack:nanojson:1.8")
    implementation("org.apache.maven.resolver:maven-resolver-supplier:1.9.15")
    implementation("org.apache.maven:maven-resolver-provider:3.9.4")
}

sourceSets {
    main {
        blossom {
            javaSources {
                property("version", project.version.toString())
            }
        }
    }
}

tasks.build {
    finalizedBy("shadowJar")
}

tasks.withType<ShadowJar> {
    archiveClassifier.set("")

    dependencies {
        include(dependency("com.grack:nanojson"))
    }

    relocate("com.grack.nanojson", "com.alessiodp.libby.nanojson")
}