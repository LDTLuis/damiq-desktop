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

## Dados locais

O banco SQLite (`damiq.db`) e os arquivos de configuração ficam no diretório de dados do usuário:
`%APPDATA%\DAMIQ` no Windows e `~/.local/share/damiq` no Linux (ou `-Ddamiq.dados.diretorio=<caminho>`).
O esquema é criado e atualizado pelo Flyway ao iniciar, a partir de
[`db/migracao`](infraestrutura/src/main/resources/db/migracao).

Até a Central existir, a configuração de cada barragem é lida de `<dados>/configuracoes/<id da barragem>.json`,
no formato da seção `configuracao` do contrato do motor, e só passa a valer depois de validada pelo motor.

## Importação de leituras (CSV/XLSX)

Arquivos da equipe de campo (RF-03): CSV (`.csv`, `.txt`) ou planilha Excel (`.xlsx`, `.xls`, primeira aba),
com uma linha de títulos e uma leitura por linha.

| Coluna | Títulos aceitos | Exemplo |
|---|---|---|
| instrumento | instrumento, sensor, código, ponto | `PZ-01` |
| tipo | tipo, grandeza | `Pressão`, `nivel`, `VAZÃO` |
| data e hora | data_hora, data/hora, timestamp, momento — ou **data** e **hora** separadas | `29/09/2026 08:00`, `2026-09-29T08:00:00-03:00` |
| valor | valor, leitura, medição | `1,4` ou `1.4` |
| unidade | unidade, unid., un | `bar`, `kPa`, `m³/s` |

- Os títulos não diferenciam maiúsculas, acentos e pontuação; colunas extras (ex.: observação) são ignoradas.
- CSV: separador `;`, `,` ou tabulação; UTF-8 ou Windows-1252 (o padrão do Excel em português).
- Datas no formato brasileiro são convertidas para ISO-8601; sem fuso, vale o `fuso_padrao` da configuração.
- Linhas em branco são ignoradas. Uma linha com problema não impede a importação: ela volta como rejeição,
  com o número da linha e o motivo.

## Notificações de alerta (RF-06)

Cada alerta gravado gera uma notificação no app. Enquanto ela não for reconhecida, novos episódios do mesmo
problema (barragem, instrumento e tipo de alerta) entram nela como ocorrências e, se forem mais graves, a
escalam. Depois do reconhecimento (quem, quando e observação, para auditoria), o próximo episódio abre uma
notificação nova. Os eventos `NotificacaoEmitida` e `NotificacaoReconhecida` saem pelo Spring Events, para a
interface exibir.

## Arquitetura

As regras são verificadas pela tarefa `verificarArquitetura`, que roda no `check`/`build`.
As versões das bibliotecas ficam em [`gradle/libs.versions.toml`](gradle/libs.versions.toml) e as convenções
comuns (Java 21, UTF-8, JUnit) em [`build-logic`](build-logic/src/main/kotlin/damiq.java.gradle.kts).
