repositories {
    maven("https://hub.spigotmc.org/nexus/content/groups/public/")
}

dependencies {
    api(project(":libby-core"))

    compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")
}
