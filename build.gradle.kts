plugins {
    base
}

// Regras de dependência da arquitetura hexagonal (docs/desktop/decisoes-back-desktop.md, seção 3).
// Cada módulo só pode depender dos módulos listados; `dominio` também não pode ter dependências externas.
val dependenciasPermitidas = mapOf(
    "dominio" to emptySet(),
    "aplicacao" to setOf("dominio"),
    "infraestrutura" to setOf("dominio", "aplicacao"),
    "inicializacao" to setOf("dominio", "aplicacao", "infraestrutura"),
)
val semDependenciasExternas = setOf("dominio")
val configuracoesDeProducao = setOf("api", "implementation", "compileOnly", "compileOnlyApi", "runtimeOnly")

val verificarArquitetura = tasks.register("verificarArquitetura") {
    group = "verification"
    description = "Verifica se os módulos respeitam as regras de dependência da arquitetura."

    val violacoes = provider {
        subprojects.flatMap { modulo ->
            val permitidos = dependenciasPermitidas[modulo.name]
                ?: return@flatMap listOf("${modulo.name}: módulo sem regra em dependenciasPermitidas")
            val dependencias = modulo.configurations
                .filter { it.name in configuracoesDeProducao }
                .flatMap { it.dependencies }

            val internas = dependencias.filterIsInstance<ProjectDependency>()
                .map { it.path.removePrefix(":") }
                .filter { it !in permitidos }
                .map { "${modulo.name} -> $it (proibido)" }
            val externas = if (modulo.name in semDependenciasExternas) {
                dependencias.filterIsInstance<ExternalModuleDependency>()
                    .map { "${modulo.name} -> ${it.group}:${it.name} (dependência externa proibida)" }
            } else {
                emptyList()
            }
            (internas + externas).distinct()
        }
    }
    inputs.property("violacoes", violacoes)

    doLast {
        val encontradas = violacoes.get()
        if (encontradas.isNotEmpty()) {
            throw GradleException(
                "Violações da arquitetura:\n" + encontradas.joinToString("\n") { "  - $it" }
            )
        }
    }
}

tasks.named("check") {
    dependsOn(verificarArquitetura)
}
