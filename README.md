# DAMIQ Desktop

Aplicativo Desktop do DAMIQ (monitoramento de barragens): registro e importação de medições, integração com o
[motor de cálculo](https://github.com/LDTLuis/modulo-calculo-api), alertas, cadastros e relatórios.

Decisões e escopo: [`docs/desktop/decisoes-back-desktop.md`](https://github.com/LDTLuis/modulo-calculo-api/blob/main/docs/desktop/decisoes-back-desktop.md)
no repositório do motor.

## Requisitos

- JDK 21 (LTS)
- Não é preciso instalar o Gradle: use o wrapper (`gradlew`) versionado no repositório.
- Python 3.13 com o motor de cálculo, num ambiente `.motor` na raiz do repositório (até o empacotamento do
  motor, [issue #14](https://github.com/LDTLuis/modulo-calculo-api/issues/14)):

  ```bash
  py -3.13 -m venv .motor
  .motor/Scripts/python -m pip install https://github.com/LDTLuis/modulo-calculo-api/releases/download/v1.0.1/damiq_calc-1.0.1-py3-none-any.whl
  ```

  No Linux, use `python3.13` e `.motor/bin/python`. Para usar outro Python, passe
  `-Ddamiq.motor.python=<caminho>` ao aplicativo e defina `DAMIQ_MOTOR_PYTHON` para os testes.
  Sem o motor, os testes de integração são pulados (no CI, com `DAMIQ_MOTOR_OBRIGATORIO=true`, falham).

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
