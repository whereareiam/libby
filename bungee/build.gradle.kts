repositories {
    maven("https://hub.spigotmc.org/nexus/content/groups/public/")
}

dependencies {
    api(project(":libby-core"))

    implementation("net.md-5:bungeecord-api:1.20-R0.1-SNAPSHOT")
}