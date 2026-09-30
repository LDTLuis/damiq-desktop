// Casos de uso e portas (interfaces) que a infraestrutura implementa.
plugins {
    id("damiq.java")
}

dependencies {
    api(project(":dominio"))

    implementation(libs.slf4j.api)

    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
}
