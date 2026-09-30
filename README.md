# DAMIQ Desktop

Aplicativo Desktop do DAMIQ (monitoramento de barragens): registro e importação de medições, integração com o
[motor de cálculo](https://github.com/LDTLuis/modulo-calculo-api), alertas, cadastros e relatórios.

Decisões e escopo: [`docs/desktop/decisoes-back-desktop.md`](https://github.com/LDTLuis/modulo-calculo-api/blob/main/docs/desktop/decisoes-back-desktop.md)
no repositório do motor.

## Requisitos

- JDK 21 (LTS)
- Não é preciso instalar o Gradle: use o wrapper (`gradlew`) versionado no repositório.

## Comandos

```bash
./gradlew build                   # compila, roda os testes e verifica a arquitetura
./gradlew verificarArquitetura    # só as regras de dependência entre módulos
./gradlew :inicializacao:run      # sobe o aplicativo
```

No Windows (PowerShell/cmd), use `gradlew.bat` ou `.\gradlew`.

## Estrutura

| Módulo | Conteúdo | Pode depender de |
|---|---|---|
| `dominio` | Entidades e regras: Barragem, Instrumento, Medição, Alerta, Configuração | nada (nem bibliotecas externas) |
| `aplicacao` | Casos de uso e portas (interfaces) | `dominio` |
| `infraestrutura` | Adapters: motor de cálculo, SQLite, Central | `aplicacao`, `dominio` |
| `inicializacao` | Montagem do Spring Context e ponto de entrada | todos |

As regras são verificadas pela tarefa `verificarArquitetura`, que roda no `check`/`build`.
As versões das bibliotecas ficam em [`gradle/libs.versions.toml`](gradle/libs.versions.toml) e as convenções
comuns (Java 21, UTF-8, JUnit) em [`build-logic`](build-logic/src/main/kotlin/damiq.java.gradle.kts).
