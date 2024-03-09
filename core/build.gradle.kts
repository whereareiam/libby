import groovy.xml.MarkupBuilder
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Base64

plugins {
    id("net.kyori.blossom") version "2.0.1"
}

val libbyMavenResolverRepo = layout.buildDirectory.dir("libby-maven-resolver-repo").get()
val libbyMavenResolverJar = provider {}.flatMap { // Use a provider to not resolve shadowTask task too early
    project(":libby-maven-resolver").tasks.named("shadowJar").flatMap { (it as Jar).archiveFile }
}

val deleteLibbyMavenResolver = tasks.register<Delete>("deleteLibbyMavenResolver") {
    delete(libbyMavenResolverRepo)
    isFollowSymlinks = false
}

val copyLibbyMavenResolver = tasks.register<Copy>("copyLibbyMavenResolver") {
    dependsOn(":libby-maven-resolver:shadowJar")
    dependsOn(deleteLibbyMavenResolver)
}
setupCopy()

tasks.test {
    dependsOn(copyLibbyMavenResolver)
}

sourceSets {
    main {
        blossom {
            javaSources {
                property("version", project.version.toString())
                property("libbyMavenResolverChecksum", provider {
                    val md = MessageDigest.getInstance("SHA-256")
                    val sha256 = md.digest(libbyMavenResolverJar.get().asFile.readBytes())
                    Base64.getEncoder().encodeToString(sha256)
                })
            }
        }
    }
    test {
        blossom {
            javaSources {
                property("buildDir", layout.buildDirectory.asFile.get().absolutePath.replace("\\", "\\\\"))
                property("libbyMavenResolverRepo", libbyMavenResolverRepo.asFile.absolutePath.replace("\\", "\\\\"))
            }
        }
    }
}

tasks.named("generateJavaTemplates") {
    dependsOn(":libby-maven-resolver:shadowJar")
}

fun setupCopy() {
    val file = libbyMavenResolverJar.map { it.asFile }
    val version = project.version.toString()
    val partialPath = "${project.group.toString().replace('.', '/')}/libby-maven-resolver/${version}"
    val mainFolder = libbyMavenResolverRepo.dir(partialPath)

    copyLibbyMavenResolver {
        from(libbyMavenResolverJar)
        into(mainFolder)
    }

    if (!version.endsWith("-SNAPSHOT")) {
        return
    }

    // Generate snapshot's maven-metadata.xml
    val timestamp = file.map { SimpleDateFormat("yyyyMMdd.HHmmss").format(it.lastModified()) }
    copyLibbyMavenResolver {
        doFirst {
            mainFolder.file("maven-metadata.xml").asFile.printWriter().use {
                val builder = MarkupBuilder(it)
                builder.doubleQuotes = true
                builder.withGroovyBuilder {
                    "metadata"("modelVersion" to "1.1.0") {
                        "groupId"(project.group.toString())
                        "artifactId"("libby-maven-resolver")
                        "version"(version)
                        "versioning"() {
                            "snapshot"() {
                                "timestamp"(timestamp.get())
                                "buildNumber"("1")
                            }
                        }
                    }
                }
            }
        }
    }

    // Rename copied file
    val newVer = timestamp.map { version.substring(0, version.length - "-SNAPSHOT".length) + '-' + it + "-1" }
    return copyLibbyMavenResolver {
        rename {
            "libby-maven-resolver-${newVer.get()}.jar"
        }
    }
}
