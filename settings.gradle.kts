pluginManagement {
    includeBuild("build-logic")
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "damiq-desktop"

include(
    "dominio",
    "aplicacao",
    "infraestrutura",
    "inicializacao",
)
