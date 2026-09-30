// Adapters: motor de cálculo (ProcessBuilder), SQLite e, no futuro, a Central.
plugins {
    id("damiq.java")
}

dependencies {
    implementation(project(":aplicacao"))
    implementation(project(":dominio"))

    implementation(libs.slf4j.api)

    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
}
