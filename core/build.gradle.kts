plugins {
    id("net.kyori.blossom") version "2.0.1"
}

dependencies {
    compileOnly("org.apache.maven.resolver:maven-resolver-supplier:1.9.15")
    compileOnly("org.apache.maven:maven-resolver-provider:3.9.4")
}

sourceSets {
    main {
        blossom {
            javaSources {
                property("version", project.version.toString())
            }
        }
    }
    test {
        blossom {
            javaSources {
                property("buildDir", layout.buildDirectory.asFile.get().absolutePath.replace("\\", "\\\\"))
            }
        }
    }
}