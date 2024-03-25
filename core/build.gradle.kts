plugins {
    id("net.kyori.blossom") version "2.0.1"
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
