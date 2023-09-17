repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    api(project(":libby-core"))

    implementation("com.velocitypowered:velocity-api:3.1.1") // Higher version requires Java 17
}