rootProject.name = "libby"

setOf(
    "bukkit",
    "bungee",
    "core",
    "nukkit",
    "paper",
    "sponge",
    "velocity"
).forEach {
    subProject(it)
}

fun subProject(name: String) {
    include(":libby-$name")
    project(":libby-$name").projectDir = file(name)
}