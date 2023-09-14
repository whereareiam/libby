plugins {
    `java-library`
    `maven-publish`
}

allprojects {
    group = "com.alessiodp.libby"
    version = "2.0.0-SNAPSHOT"

    repositories {
        mavenLocal()
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8

        withJavadocJar()
    }

    publishing {
        publications {
            create<MavenPublication>("mavenJar") {
                from(components["java"])
            }
        }
    }
}