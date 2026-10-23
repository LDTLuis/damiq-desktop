# Cadastro de usuários: contrato entre a Central e o Desktop

Os **usuários são cadastrados na Central de Configurações pelo administrador** (RF-01). Cada aparelho recebe
uma **cópia** e confere a senha **localmente**, para que o acesso funcione sem rede. Cada usuário pertence a
**uma única barragem**: ao entrar, o Desktop já abre nela, sem tela de seleção, e o usuário só vê e altera os
dados dela.

Até a API da Central existir, a publicação é um arquivo JSON local
(`<dados>/cadastro/usuarios.json`, ou `-Ddamiq.usuarios.arquivo=<caminho>`). Exemplo:
[`cadastro-usuarios.exemplo.json`](cadastro-usuarios.exemplo.json) (senha provisória dos três usuários de
exemplo: `Provisoria2026`; não use em produção).

## Formato

```json
{
  "usuarios": [
    {
      "login": "eng.joaoleite",
      "versao": 4,
      "barragem": "joao-leite",
      "perfil": "ENGENHEIRO",
      "nome": "Engenheira Responsável",
      "email": "eng.joaoleite@exemplo.com.br",
      "telefone": "(62) 99999-0001",
      "cargo": "Engenheira de segurança de barragens",
      "registro_profissional": "CREA-GO 000000/D",
      "senha_hash": "$2a$10$xLS1W84muMQxPyNj2Sc/QOyVSo8o0GATq/lTMK42I5jWzJQ7Oy.YC",
      "bloqueado": false,
      "trocar_senha": false
    }
  ]
}
```

A lista é **completa**: traz todos os usuários que o Desktop deve ter. O Desktop sincroniza os usuários
**depois** das barragens.

| Campo | Obrigatório | Regra |
|---|---|---|
| `login` | Sim | 3 a 100 caracteres: letras sem acento, números, `.`, `_`, `@`, `-`; começa por letra ou número. Sem diferença entre maiúsculas e minúsculas. Único |
| `versao` | Sim | Texto ou inteiro; mude a cada edição publicada (inclusive ao redefinir a senha ou desbloquear) |
| `barragem` | Sim | `id` de uma barragem do cadastro de barragens |
| `perfil` | Sim | `ADMINISTRADOR`, `ENGENHEIRO` ou `TECNICO_CAMPO` |
| `nome` | Sim | |
| `email` | Sim | Um `@`, sem espaços |
| `telefone`, `cargo` | Não | |
| `registro_profissional` | Para `ENGENHEIRO` | Número do CREA (assina laudos com ART) |
| `senha_hash` | Sim | **bcrypt** (`$2a$`, `$2b$` ou `$2y$`), custo recomendado 12. A Central nunca publica a senha |
| `bloqueado` | Não (falso) | Suspende o acesso sem excluir o usuário |
| `trocar_senha` | Não (falso) | Obriga a troca no próximo acesso; use ao criar o usuário ou redefinir a senha |

**CPF não é coletado** (LGPD, princípio da necessidade): o login identifica o usuário e o CREA identifica o
responsável técnico.

## Perfis e permissões

| Permissão | Administrador | Engenheiro | Técnico de campo |
|---|:-:|:-:|:-:|
| Consultar dados da barragem | ✓ | ✓ | ✓ |
| Registrar e importar leituras | | ✓ | ✓ |
| Reconhecer notificações | | ✓ | |
| Desbloquear usuários da barragem | ✓ | | |
| Apagar dados de teste | ✓ | | |

Permissões novas (cálculos, relatórios, emergência, cópia de segurança) entram com os casos de uso.

## Comportamento no Desktop

| Situação | O que acontece |
|---|---|
| Usuário novo na lista | Cadastrado; é de teste se a barragem dele for de teste |
| `versao` diferente da guardada | Dados, senha, bloqueio e troca obrigatória passam a ser os da Central; as tentativas malsucedidas zeram |
| Mesma `versao` | Nada muda: a senha trocada e o bloqueio por tentativas neste aparelho continuam |
| Usuário que estava excluído volta à lista | Reativado |
| Usuário saiu da lista | **Exclusão lógica**: não entra mais; a autoria dos registros dele continua |
| Cadastro inválido ou de barragem que não está no Desktop | **Recusado**: a cópia anterior desse usuário é mantida e o erro vai para o log |
| Arquivo ausente, JSON ilegível, sem `usuarios` ou nenhum cadastro válido | **Nada é alterado** |

## Acesso

- Login desconhecido e senha errada têm a **mesma mensagem**, para não revelar quais logins existem.
- **5 tentativas malsucedidas seguidas** (`-Ddamiq.acesso.limite-tentativas`) bloqueiam o usuário neste
  aparelho. O administrador da barragem desbloqueia no próprio Desktop, ou na Central publicando uma nova
  `versao` do usuário.
- Com `trocar_senha`, a sessão abre mas só permite trocar a senha. A senha nova tem de 10 a 64 caracteres, com
  letras e números, sem conter o login, e diferente da atual.
- A barragem do usuário precisa estar ativa no Desktop; se saiu da Central, o acesso é recusado.

## Em aberto

- **Senha trocada no aparelho**: vale nele até a Central publicar uma nova `versao`. O envio do hash novo à
  Central entra com a sincronização (fila de envio); até lá, a troca não chega aos outros aparelhos.
- **Chave do usuário**: hoje é o `login`. Se a Central permitir renomear logins, o contrato precisa de um
  identificador estável (ex.: UUID, que a sincronização também vai exigir).
