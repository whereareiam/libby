rootProject.name = "libby"

setOf(
    "bukkit",
    "bungee",
    "core",
    "fabric",
    "maven-resolver",
    "nukkit",
    "paper",
    "sponge",
    "standalone",
    "velocity"
).forEach {
    subProject(it)
}

fun subProject(name: String) {
    include(":libby-$name")
    project(":libby-$name").projectDir = file(name)
}