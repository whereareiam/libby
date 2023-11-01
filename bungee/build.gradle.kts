repositories {
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://hub.spigotmc.org/nexus/content/groups/public/")
    maven("https://libraries.minecraft.net/")
}

dependencies {
    api(project(":libby-core"))

    compileOnly("net.md-5:bungeecord-api:1.20-R0.2-SNAPSHOT")
}