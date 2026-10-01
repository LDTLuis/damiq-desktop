// Adapters: motor de cálculo (ProcessBuilder), SQLite e, no futuro, a Central.
plugins {
    id("damiq.java")
}

dependencies {
    implementation(project(":aplicacao"))
    implementation(project(":dominio"))

    implementation(platform(libs.jackson.bom))
    implementation(libs.jackson.databind)
    implementation(libs.sqlite.jdbc)
    implementation(libs.flyway.core)
    implementation(libs.poi.ooxml)
    implementation(libs.slf4j.api)

    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testRuntimeOnly(libs.logback.classic)
    testRuntimeOnly(libs.log4j.to.slf4j)
}

tasks.test {
    // Testes de integração com o motor real: usam o Python de DAMIQ_MOTOR_PYTHON ou do .motor na raiz
    // (DAMIQ_MOTOR_OBRIGATORIO=true faz falhar em vez de pular quando o motor não está instalado, como no CI)
    systemProperty("damiq.raiz", rootDir.absolutePath)
    inputs.property("motorPython", providers.environmentVariable("DAMIQ_MOTOR_PYTHON").orElse(""))
    inputs.property("motorObrigatorio", providers.environmentVariable("DAMIQ_MOTOR_OBRIGATORIO").orElse(""))
}
