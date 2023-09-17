repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    api(project(":libby-core"))

    implementation("io.papermc.paper:paper-api:1.19.4-R0.1-SNAPSHOT") // Higher version requires Java 17
}