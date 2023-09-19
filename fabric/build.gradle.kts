repositories {
    maven("https://maven.fabricmc.net")
}

dependencies {
    api(project(":libby-core"))

    implementation("net.fabricmc:fabric-loader:0.14.21")
    compileOnly("org.slf4j:slf4j-api:2.0.5") // Not api of fabric-loader
}