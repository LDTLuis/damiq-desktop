// Montagem do aplicativo: Spring Context que conhece todos os módulos.
plugins {
    id("damiq.java")
    application
}

dependencies {
    implementation(project(":dominio"))
    implementation(project(":aplicacao"))
    implementation(project(":infraestrutura"))

    implementation(libs.spring.context)
    implementation(libs.slf4j.api)
    runtimeOnly(libs.logback.classic)
}

application {
    mainClass = "br.com.damiq.desktop.inicializacao.DamiqDesktop"
}
