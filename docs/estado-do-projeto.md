# DAMIQ Desktop — estado do projeto e guia para continuar

Registro de 01/10/2026, atualizado depois do PR #12 (contatos do PAE). Ponto de partida para continuar o desenvolvimento do back do
Desktop em outra conversa: o que existe, as decisões tomadas, como trabalhar e o que falta.

> **Para iniciar uma nova conversa:** peça para ler este arquivo e o
> [`decisoes-back-desktop.md`](https://github.com/LDTLuis/modulo-calculo-api/blob/main/docs/desktop/decisoes-back-desktop.md)
> do repositório do motor (seções 1 a 6), e então indique a próxima entrega (seção 8).

---

## 1. Contexto

| Item | Onde |
|---|---|
| Repositório do Desktop | [`LDTLuis/damiq-desktop`](https://github.com/LDTLuis/damiq-desktop) (público), pasta local `Projeto-DAMIQ/damiq-desktop` |
| Motor de cálculo (Python) | [`LDTLuis/modulo-calculo-api`](https://github.com/LDTLuis/modulo-calculo-api), release **v1.0.1**, contrato JSON **1.0** |
| Decisões iniciais do back | `docs/desktop/decisoes-back-desktop.md` no repositório do motor |
| Requisitos, stack, PAE, apostila | `Projeto-DAMIQ/1. Documentos/` (requisitos RF-01 a RF-13, `Stack DAMIQ.xlsx`, `Documentos Professor/`) |
| Guia da configuração da Central | `Projeto-DAMIQ/1. Documentos/Documentos Central de Configuração Web/guia-configuracao-v1.pdf` |
| Contrato do cadastro de barragens | [`docs/central/cadastro-barragens.md`](central/cadastro-barragens.md) (neste repositório) |

**Stack:** Java 21, Gradle 9.8 (wrapper), Spring Context + Spring Events, SQLite JDBC + Flyway 13, Jackson 3,
SLF4J + Logback, JUnit 5 + Mockito, Apache POI 5.5. Sem Spring Boot nem JPA (JDBC puro).

## 2. Como rodar

```bash
# motor (uma vez): Python 3.13 com o wheel da release, em .motor na raiz
py -3.13 -m venv .motor
.motor/Scripts/python -m pip install https://github.com/LDTLuis/modulo-calculo-api/releases/download/v1.0.1/damiq_calc-1.0.1-py3-none-any.whl

./gradlew build                 # compila, testa (inclusive com o motor real) e verifica a arquitetura
./gradlew :inicializacao:run    # sobe o aplicativo
```

- `DAMIQ_MOTOR_OBRIGATORIO=true` faz os testes de integração falharem (em vez de serem pulados) sem o motor —
  é assim no CI.
- Dados do usuário: `%APPDATA%\DAMIQ` (Windows) ou `~/.local/share/damiq` (Linux); `-Ddamiq.dados.diretorio`
  para outro lugar. Lá ficam `damiq.db`, `configuracoes/<barragem>.json` e `cadastro/barragens.json`.
- Para testar o app sem mexer nos seus dados, use uma pasta temporária:
  `JAVA_OPTS="-Ddamiq.dados.diretorio=<tmp> -Ddamiq.motor.python=<raiz>/.motor/Scripts/python.exe" inicializacao/build/install/inicializacao/bin/inicializacao`
  (depois de `./gradlew :inicializacao:installDist`; no Git Bash, use caminhos com `/`).

Propriedades (`-D`): `damiq.motor.python`, `damiq.motor.tempo-limite-s` (60), `damiq.motor.historico-por-instrumento`
(48), `damiq.dados.diretorio`, `damiq.banco.arquivo`, `damiq.configuracao.diretorio`, `damiq.cadastro.arquivo`.

## 3. Arquitetura

DDD + hexagonal, em módulos Gradle; a tarefa `verificarArquitetura` (no `build`) quebra o build se as regras
forem violadas.

| Módulo | Conteúdo | Depende de |
|---|---|---|
| `dominio` | Entidades e regras, Java puro (records/enums) | nada, nem bibliotecas |
| `aplicacao` | Casos de uso e portas (interfaces) | `dominio` (+ SLF4J) |
| `infraestrutura` | Adapters: motor (ProcessBuilder), SQLite (JDBC/Flyway), arquivos, CSV/XLSX | `aplicacao`, `dominio` |
| `inicializacao` | Spring Context (`ConfiguracaoAplicacao`), `DamiqDesktop` (main), ouvintes de eventos | todos |
| `ui/` (futuro) | JavaFX (Pedro) | só `aplicacao` |

Pacote base `br.com.damiq.desktop`. Versões em `gradle/libs.versions.toml`; convenções em
`build-logic/src/main/kotlin/damiq.java.gradle.kts`.

### O que existe (casos de uso)

| Área | Casos de uso / peças | PR |
|---|---|---|
| Motor | Porta `MotorCalculo` (`info`, `validarConfiguracao`, `processarLote`), adapter `MotorCalculoProcessBuilder` (arquivos UTF-8, tempo limite, encerra processos filhos, códigos de saída 0/1/2), DTOs `RequisicaoMotor`/`RespostaMotor` escritos à mão + `TraducaoContrato`; `VerificarCompatibilidadeMotor` (contrato 1.x) | #3, #5 |
| Configuração | `AtualizarConfiguracao`: lê `configuracoes/<barragem>.json`, valida no motor, ativa se válida, senão mantém a vigente | #4 |
| Medições | `ProcessarMedicoes`: configuração vigente + histórico (48 por instrumento) → motor → grava processamento, medições, rejeições, lacunas, alertas numa transação; reimportação não duplica | #6 |
| Importação | `ImportarMedicoes` + `LeitorArquivoLeiturasPadrao`: CSV (`;`/`,`/tab, UTF-8/Windows-1252) e XLSX/XLS; títulos flexíveis; rejeições com a linha do arquivo | #7 |
| Notificações | `NotificarAlertas` (uma aberta por barragem+instrumento+tipo; escala se mais grave; idempotente, recupera pendentes ao iniciar), `ReconhecerNotificacao`, `ConsultarNotificacoes`; eventos pelo Spring Events | #8 |
| Banco | Colunas de controle, `usuario` 1 = sistema, exclusão lógica, `LimparDadosTeste`, `ExcluirBarragem` | #9 |
| Barragens | `SincronizarBarragens` (cópia do cadastro da Central), `ConsultarBarragens`; catálogo padrão + grupos por barragem | #10 |
| Contatos do PAE | `contatos` no cadastro da Central (papel, meios na ordem de preferência, nível de acionamento, substituto); `ConsultarAcionamento`: quem acionar, em que ordem, para uma notificação ou nível, com pendências do cadastro | #12 |

Eventos (Spring Events, síncronos): `MedicoesProcessadas` → `OuvinteNotificacoes` → `NotificarAlertas` →
`NotificacaoEmitida` (`NOVA`/`ESCALADA`/`REPETIDA`); `NotificacaoReconhecida`. A interface vai ouvir os de
notificação.

Inicialização (`DamiqDesktop`): sobe o contexto (Flyway migra o banco) → `SincronizarBarragens` →
`VerificarCompatibilidadeMotor` → `NotificarAlertas.executarPendentes`.

### Banco (migrações em `infraestrutura/src/main/resources/db/migracao`)

| Migração | Conteúdo |
|---|---|
| V1 | `barragem`, `configuracao` (histórico de versões, uma vigente por barragem) |
| V2 | `processamento`, `medicao` (única por barragem+instrumento+momento), `rejeicao`, `lacuna`, `alerta` |
| V3 | `notificacao`, `alerta.notificacao_id` |
| V4 | `usuario`; recria todas as tabelas com as **colunas de controle**; unicidades viram índices parciais; gatilhos contra exclusão física |
| V5 | Cadastro na `barragem` (campos obrigatórios, `versao_cadastro`), `barragem_grupo`, `barragem_campo` |
| V6 | `barragem_contato` (contatos do PAE; meios em JSON) |

Convenções: datas em texto ISO-8601 **UTC** com milissegundos (`2026-09-29T14:00:00.000Z`) + coluna `fuso`
quando o fuso original importa; booleanos INTEGER 0/1; tabelas `STRICT`; valores fixos em TEXT com CHECK.

## 4. Decisões tomadas (não rediscutir sem motivo)

1. **A barragem é o cadastro principal**: todo dado, de cálculo ou de usuário, pertence a uma barragem
   (`barragem_id` ou FK que chega a ela).
2. **Colunas de controle em toda tabela**: `criado_em`, `criado_por`, `atualizado_em`, `atualizado_por`
   (FK para `usuario`; até o login, usuário 1 "sistema"), `excluido_em` (exclusão lógica; quem excluiu fica em
   `atualizado_por`) e `teste` (1 = pode ser apagado fisicamente; **herdado da barragem**). O banco recusa
   `DELETE` de registro com `teste = 0`. Unicidades valem só entre registros não excluídos. Consultas ignoram
   excluídos.
3. **Migrações já mescladas não são alteradas**: mudanças vão numa migração nova (V6…). Tabelas existentes
   que precisem de restrição nova são recriadas (padrão `_novo` + cópia + renomeação, como na V4).
4. **O cadastro de barragens é feito na Central de Configurações**; o Desktop guarda uma **cópia somente
   leitura** (como a configuração de monitoramento). Até a API existir, a publicação é um arquivo local.
5. **Campos obrigatórios do cadastro**: nome, empreendedor, finalidade, município(s), UF, coordenadas, curso
   d'água, tipo do maciço, altura do maciço (fundação à crista, m) e capacidade total (m³).
6. **Campos próprios** seguem um **catálogo padrão na Central** (onde se criam e editam campos); cada barragem
   tem a **sua tabela de grupos**; cada campo indica se é do catálogo (`padrao`).
7. **DTOs do motor escritos à mão** (não gerados): o `resposta.schema.json` da v1.0.1 não descreve `medicoes`,
   `lacunas`, `resumo` nem detalhes de `monitoramento`. Conformidade garantida por testes com o motor real e a
   resposta real gravada (`infraestrutura/src/test/resources/motor/`).
8. **Notificação**: uma notificação aberta por problema (barragem + instrumento + tipo de alerta); novos
   episódios somam ocorrências; mais grave = escalada; reconhecimento (quem, quando, observação) encerra.
9. **Configurações recusadas** vão só para o log (não para o banco).
10. **Importação**: formato longo (uma leitura por linha) com colunas instrumento, tipo, data_hora (ou data +
    hora), valor, unidade; o leitor não valida valores, o motor recusa a linha.
11. **Contatos do PAE vêm do cadastro da Central**, com o nível de resposta a partir do qual cada um é
    acionado. O acionamento é **cumulativo** (fluxograma do PAE, §6.1 e §7.2): verde = equipe técnica →
    coordenador do PAE → empreendedor; amarelo + entidade fiscalizadora; vermelho + Defesa Civil (que alerta a
    população). A ordem da lista é a ordem de acionamento dentro do nível. Falta de papel previsto vira
    **pendência** exibida, não recusa do cadastro. O nível vem da notificação (`nivelResposta`, do motor).
12. **Dois bancos**: a Central tem um servidor com **PostgreSQL**, que guarda tudo de forma consolidada; cada
    aparelho tem um **SQLite** local para trabalhar sem rede. O que o Desktop gera (leituras, rejeições,
    lacunas, alertas, notificações, reconhecimentos) **sincroniza com o servidor** quando há conexão. Ainda não
    implementado; ver §7.
13. **A barragem é o topo da cadeia, inclusive para o usuário**: cada usuário pertence a uma barragem e só vê e
    altera os dados dela. A barragem vem do usuário conectado, **nunca de uma escolha na tela** (não há tela de
    seleção de barragem). Os casos de uso continuam recebendo `BarragemId`; quem o fornece é a sessão (RF-01).

## 5. Como trabalhamos

- **Uma branch por entrega** (`feat/…`, `docs/…`), **PR para a `main`**, merge commit (`gh pr merge --merge
  --delete-branch`) depois do CI verde em Ubuntu **e** Windows.
- **CI** (`.github/workflows/testes.yml`): instala Python 3.13 + motor 1.0.1 e roda `./gradlew build` com
  `DAMIQ_MOTOR_OBRIGATORIO=true`. O job do Windows pode levar alguns minutos.
- **Commits e PRs em português**, com resumo do que entra, testes e o que fica de fora; commits terminam com
  `Co-Authored-By: Claude …`, PRs com `🤖 Generated with [Claude Code]`.
- **Código**: nomes em português, records para valores, validação nos construtores (`Validacao`),
  exceções específicas por porta, Javadoc curto explicando o porquê. Testes: unitários com Mockito ou fakes em
  memória, repositórios contra SQLite temporário (`@TempDir`), integração com o motor real via
  `MotorInstalado.exigir()`; `AutoriaDeTeste` para os repositórios.
- **Particularidades desta máquina (Windows)**: o git usa o OpenSSH do Windows (`core.sshCommand`) — o
  push por SSH funciona; o `ssh` do Git Bash não. Para ler os PDFs do professor: `pdftotext -layout` (texto
  sai em Latin-1). Ao passar caminhos para o app pelo Git Bash, use `cygpath -m`.
- A apostila do professor tem direito autoral: **não copiar trechos** para o repositório.

## 6. Estado dos requisitos

| Requisito | Situação |
|---|---|
| RF-01 Autenticação e perfis | **Não iniciado.** Base pronta: tabela `usuario`, porta `UsuarioCorrente` (hoje sempre "sistema"). Cada usuário pertence a uma barragem (decisão 13) |
| RF-02 Barragens | **Cópia do cadastro da Central** pronta (#10), com contatos do PAE (#12). Faltam estruturas, ZAS, instrumentos detalhados |
| RF-03 Medições | Processamento (#6) e importação CSV/XLSX (#7) prontos. Falta digitação (tela) e exclusão de medição |
| RF-04 Cálculos | `processar_lote` integrado. Faltam `listar_calculos`/`calcular` (telas de cálculo) |
| RF-05 Monitoramento/histórico | Dados gravados; faltam consultas por período/instrumento para o dashboard |
| RF-06 Alertas e notificações | Notificação no app (#8) e quem acionar por nível do PAE (#12) prontos. Falta o registro da emergência e dos acionamentos (com RF-08) e o envio automático (e-mail/SMS), se exigido |
| RF-07 Relatórios PDF | Não iniciado (OpenPDF; motor gera gráficos com `opcoes.graficos`) |
| RF-08 Incidentes | Não iniciado |
| RF-09 Dashboard | Não iniciado (UI) |
| RF-10 Exportação CSV/XLSX | Não iniciado |
| RF-11 Backup/restauração | Não iniciado |
| RF-12 Logs e auditoria | Parcial: colunas de controle e logs; falta tabela de auditoria (histórico de alterações) |
| RF-13 UAT | Base pronta: barragem de teste e `LimparDadosTeste` |

## 7. Pendências e pontos em aberto

**Dependem de terceiros**
- **Formulário oficial da SEMAD (IN 01/2020) e do SNISB**: para completar o catálogo e definir obrigatórios.
- **API da Central**: hoje configuração e cadastro vêm de arquivos locais (`FonteConfiguracaoArquivo`,
  `FonteCadastroBarragensArquivo`); trocar por clientes REST quando existir.
- **Sincronização com o servidor da Central (PostgreSQL)**: fila de envio local; identificador global (UUID)
  nas tabelas sincronizadas, porque os ids INTEGER de cada aparelho colidem no servidor (migração nova);
  reenvio seguro pela unicidade de `medicao` (barragem + instrumento + momento); **em aberto** a regra de
  conflito quando dois aparelhos alteram a mesma notificação.
- **Banco criptografado**: o documento de requisitos fala em "Local Encrypted DB"; se exigido, trocar o driver
  por SQLCipher (`sqlite-jdbc-crypt`) sem mudar o esquema. Contatos (LGPD) reforçam a questão.
- **JavaFX no mesmo repositório (`ui/`)**: aguardando o Pedro.
- **Issue no motor**: completar o `resposta.schema.json` (campos de `processar_lote` ausentes).
- Empacotamento do motor no instalador (issue #14 do motor).

**Técnicas**
- `historico-por-instrumento` fixo (48): ler `anomalia.janela_leituras`/`sensor_travado.leituras_consecutivas`
  da configuração.
- Versão do motor não é gravada no processamento (vir do `info` da inicialização).
- Número da linha do arquivo importado não é gravado em `rejeicao`.
- Excluir medição pela tela: decidir o efeito sobre alertas/notificações já gerados.
- Usuários de teste ficam fora de `LimparDadosTeste` até existir login.
- Histórico das versões do cadastro de barragens não é guardado (só a cópia atual).

## 8. Próximos passos sugeridos

1. **Consultas para o dashboard** (RF-05): séries por instrumento e período, situação atual da barragem.
2. **`listar_calculos` / `calcular`** (RF-04) com registro dos cálculos por barragem.
3. **Registro da emergência** (RF-06 + RF-08): declaração de início e encerramento (obrigatória nos níveis
   amarelo e vermelho), mensagem de notificação (PAE §11, p. 115) e registro de quem foi acionado, quando e se
   confirmou o recebimento. Usa `ConsultarAcionamento`.
4. **Tabela de auditoria** (RF-12).
5. **Autenticação e perfis** (RF-01), substituindo o usuário "sistema": usuário vinculado à sua barragem, que
   passa a ser o contexto de todas as telas (decisão 13).
6. **Sincronização com o servidor da Central** (decisão 12), quando a API existir.
7. **Relatórios PDF** (RF-07) e **exportação** (RF-10).
