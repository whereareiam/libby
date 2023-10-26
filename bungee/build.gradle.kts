repositories {
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    api(project(":libby-core"))

    implementation("net.md-5:bungeecord-api:1.20-R0.2-SNAPSHOT")
}