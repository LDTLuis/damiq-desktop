# Cadastro de barragens: contrato entre a Central e o Desktop

O **cadastro de barragens é feito na Central de Configurações**. O Desktop guarda uma **cópia somente leitura**
e a atualiza a partir da publicação da Central, como já faz com a configuração de monitoramento. A barragem é
o cadastro principal do Desktop: configurações, medições, alertas, notificações, cálculos e usuários
pertencem a uma barragem.

Até a API da Central existir, a publicação é um arquivo JSON local
(`<dados>/cadastro/barragens.json`, ou `-Ddamiq.cadastro.arquivo=<caminho>`). Exemplo completo:
[`cadastro-barragens.exemplo.json`](cadastro-barragens.exemplo.json).

## Formato

```json
{
  "barragens": [
    {
      "id": "joao-leite",
      "versao": 3,
      "nome": "Barragem do Ribeirão João Leite",
      "teste": false,
      "empreendedor": "Saneamento de Goiás S.A. - SANEAGO",
      "finalidade": "Abastecimento público",
      "municipios": ["Goiânia"],
      "uf": "GO",
      "latitude": -16.5701,
      "longitude": -49.2151,
      "curso_dagua": "Ribeirão João Leite",
      "tipo_macico": "Concreto compactado com rolo (CCR)",
      "altura_macico_m": 50,
      "capacidade_total_m3": 129000000,
      "grupos": [
        { "chave": "macico", "nome": "Maciço" },
        { "chave": "abastecimento", "nome": "Abastecimento de Goiânia" }
      ],
      "campos": [
        { "chave": "cota_crista", "rotulo": "Cota da crista", "grupo": "macico",
          "tipo": "NUMERO", "valor": 752.5, "unidade": "m", "padrao": true },
        { "chave": "vazao_abastecimento", "rotulo": "Vazão destinada ao abastecimento", "grupo": "abastecimento",
          "tipo": "NUMERO", "valor": 5.33, "unidade": "m³/s", "padrao": false }
      ]
    }
  ]
}
```

A lista é **completa**: traz todas as barragens que o Desktop deve ter.

### Campos obrigatórios

| Campo | Tipo | Regra |
|---|---|---|
| `id` | texto | Identificador estável da barragem; não muda nunca (é a chave de todos os dados no Desktop) |
| `versao` | texto ou inteiro | Muda a cada edição publicada; o Desktop só regrava quando ela muda |
| `nome` | texto | |
| `empreendedor` | texto | Responsável legal pela barragem (Lei 12.334/2010) |
| `finalidade` | texto | Ex.: abastecimento público, irrigação, geração de energia |
| `municipios` | lista de textos | Ao menos um |
| `uf` | texto | Sigla da UF (ex.: `GO`) |
| `latitude`, `longitude` | número | Graus decimais (SIRGAS 2000); latitude de -90 a 90, longitude de -180 a 180 |
| `curso_dagua` | texto | Rio ou ribeirão barrado |
| `tipo_macico` | texto | Ex.: terra, enrocamento, concreto compactado com rolo (CCR) |
| `altura_macico_m` | número > 0 | Altura do maciço **do ponto mais baixo da fundação à crista**, em metros (critério da PNSB) |
| `capacidade_total_m3` | número > 0 | Capacidade total do reservatório em **m³** (não hm³) |

`altura_macico_m` e `capacidade_total_m3` alimentam o enquadramento na PNSB calculado pelo motor
(`classificacao.enquadramento_pnsb`).

### Opcionais

| Campo | Tipo | Regra |
|---|---|---|
| `teste` | booleano | Barragem de teste (UAT, RF-13): todos os seus dados no Desktop são de teste e podem ser apagados. **Só vale no primeiro cadastro** |
| `grupos` | lista | Grupos de campos desta barragem (abaixo) |
| `campos` | lista | Campos próprios desta barragem (abaixo) |
| `contatos` | lista | Contatos do PAE e ordem de acionamento ([abaixo](#contatos-do-pae-e-ordem-de-acionamento)) |

## Catálogo padrão, grupos e campos próprios

### Catálogo (na Central)

A Central mantém um **catálogo padrão** de grupos e campos (chave, rótulo, grupo, tipo e unidade), para que a
mesma característica tenha o mesmo nome em todas as barragens e possa ser usada em comparações e cálculos.
Na Central é possível **criar campos novos e editar os existentes** no catálogo, e também criar campos só para
uma barragem.

Ao cadastrar uma barragem, a Central oferece os grupos e campos do catálogo; o usuário preenche os valores,
pode renomear, reordenar ou criar grupos para aquela barragem e incluir campos próprios fora do catálogo.

Ao **editar um campo do catálogo** (ex.: corrigir um rótulo), a Central deve republicar, com versão nova, as
barragens que usam o campo. O Desktop não recebe o catálogo: recebe cada barragem já resolvida.

**Catálogo inicial sugerido** (ficha técnica do PAE, §3):

| Grupo | Chave | Rótulo | Tipo | Unidade |
|---|---|---|---|---|
| `macico` Maciço | `comprimento_crista` | Comprimento da crista | NUMERO | m |
| | `cota_crista` | Cota da crista | NUMERO | m |
| | `largura_crista` | Largura da crista | NUMERO | m |
| `vertedouro` Vertedouro | `tipo_vertedouro` | Tipo do vertedouro | TEXTO | |
| | `cota_soleira_vertedouro` | Cota da soleira do vertedouro | NUMERO | m |
| | `largura_vertedouro` | Largura do vertedouro | NUMERO | m |
| | `vazao_cheia_projeto` | Vazão da cheia de projeto | NUMERO | m³/s |
| | `tempo_retorno_cheia_projeto` | Tempo de retorno da cheia de projeto | NUMERO | anos |
| | `na_maximo_maximorum` | NA máximo maximorum | NUMERO | m |
| `reservatorio` Reservatório | `na_maximo_normal` | NA máximo normal | NUMERO | m |
| | `area_inundada_na_normal` | Área inundada no NA máximo normal | NUMERO | km² |
| | `volume_util` | Volume útil | NUMERO | hm³ |
| | `volume_morto` | Volume morto | NUMERO | hm³ |
| `hidrologia` Hidrologia | `area_drenagem` | Área de drenagem | NUMERO | km² |
| | `precipitacao_media_anual` | Precipitação média anual | NUMERO | mm |
| | `vazao_media_longo_periodo` | Vazão média de longo período | NUMERO | m³/s |
| | `vazao_q95` | Vazão Q95 | NUMERO | m³/s |
| `historico` Histórico | `conclusao_obra` | Conclusão da obra | DATA | |
| | `inicio_enchimento` | Início do enchimento | DATA | |
| `seguranca` Segurança | `possui_pae` | Possui PAE | BOOLEANO | |
| | `categoria_risco` | Categoria de risco (CRI) | TEXTO | |
| | `dano_potencial` | Dano potencial associado (DPA) | TEXTO | |
| `regularizacao` Regularização | `outorga` | Outorga de uso da água | TEXTO | |
| | `licenca_ambiental` | Licença ambiental | TEXTO | |
| | `codigo_snisb` | Código no SNISB | TEXTO | |

### Grupos (`grupos`)

Cada barragem tem a **sua tabela de grupos**, na ordem de exibição.

| Campo | Obrigatório | Regra |
|---|---|---|
| `chave` | sim | Identificador estável do grupo na barragem: minúsculas, números e `_`, começando por letra, até 64 caracteres. Única na barragem |
| `nome` | sim | Nome exibido (ex.: "Maciço") |

### Campos (`campos`)

| Campo | Obrigatório | Regra |
|---|---|---|
| `chave` | sim | Identificador estável do campo na barragem (mesma regra da chave do grupo). Única na barragem. Nos campos do catálogo, é a chave do catálogo |
| `rotulo` | sim | Nome exibido (ex.: "Cota da crista") |
| `grupo` | não | Chave de um grupo **desta barragem**; sem grupo, o campo aparece à parte |
| `tipo` | sim | `NUMERO`, `TEXTO`, `DATA` ou `BOOLEANO` |
| `valor` | sim | `NUMERO`: número com ponto decimal; `DATA`: `AAAA-MM-DD`; `BOOLEANO`: `true`/`false`; `TEXTO`: livre |
| `unidade` | não | Ex.: `m`, `hm³`, `m³/s`, `km²` |
| `padrao` | não | `true` se o campo vem do catálogo; `false` (padrão) se foi criado só para esta barragem |

A ordem das listas é a ordem de exibição (grupos, e campos dentro de cada grupo).

## Contatos do PAE e ordem de acionamento

Cada barragem traz a lista de contatos do seu PAE (identificação e contatos, PAE §2; responsabilidades, §7;
entidades que recebem cópia, §12) e, para cada um, o **nível de resposta a partir do qual ele é acionado**.
O Desktop usa essa lista para mostrar, em cada notificação de alerta, **quem acionar e em que ordem** (RF-06).

O fluxograma de notificação do PAE é cumulativo: cada nível aciona os contatos do nível anterior e mais alguns.

| Nível | Quem é acionado (PAE §6.1 e §7.2) |
|---|---|
| 1 – verde | Equipe técnica → coordenador do PAE → empreendedor. A notificação é interna e termina no empreendedor |
| 2 – amarelo | + entidade fiscalizadora (ex.: SEMAD). O coordenador do PAE mobiliza a operação de emergência |
| 3 – vermelho | + Defesa Civil municipal e estadual, que alertam a população da ZAS. O coordenador **não** fala direto com a população |

```json
"contatos": [
  { "chave": "coordenador_pae", "papel": "COORDENADOR_PAE", "entidade": "Empresa de Saneamento",
    "responsavel": "Nome do coordenador", "cargo": "Coordenador do PAE",
    "meios": [ { "tipo": "CELULAR", "valor": "(62) 99999-0000" }, { "tipo": "EMAIL", "valor": "pae@empresa.com.br" } ],
    "nivel_acionamento": 1, "recebe_copia_pae": true },
  { "chave": "coordenador_pae_substituto", "papel": "COORDENADOR_PAE", "entidade": "Empresa de Saneamento",
    "responsavel": "Nome do substituto", "meios": [ { "tipo": "CELULAR", "valor": "(62) 99999-0001" } ],
    "substitui": "coordenador_pae" },
  { "chave": "defesa_civil_municipal", "papel": "DEFESA_CIVIL", "entidade": "Defesa Civil Municipal",
    "meios": [ { "tipo": "TELEFONE", "valor": "199" } ], "nivel_acionamento": 3 }
]
```

| Campo | Obrigatório | Regra |
|---|---|---|
| `chave` | sim | Identificador estável do contato na barragem (mesma regra da chave do grupo). Única na barragem |
| `papel` | sim | `EQUIPE_TECNICA`, `COORDENADOR_PAE`, `EMPREENDEDOR`, `ENTIDADE_FISCALIZADORA`, `DEFESA_CIVIL`, `CONSULTOR_EXTERNO` ou `OUTRO` |
| `entidade` | sim | Órgão ou empresa |
| `responsavel` | não | Pessoa a contatar. O PAE pede o contato **direto com o responsável** (§6.1.2.2); informe sempre que houver |
| `cargo` | não | |
| `meios` | se acionado | Lista **na ordem de preferência**, cada um com `tipo` (`CELULAR`, `TELEFONE`, `EMAIL`, `RADIO`, `OUTRO`) e `valor`. O PAE recomenda o celular de viva voz e o e-mail como complemento. Obrigatório (ao menos um) para contatos acionados e para substitutos de contatos acionados |
| `nivel_acionamento` | não | `1`, `2` ou `3`: nível a partir do qual o contato é acionado. Ausente: o contato não está no fluxo de notificação (ex.: só recebe cópia do PAE) |
| `substitui` | não | Chave do titular que este contato substitui quando o titular não é encontrado (ex.: coordenador substituto do PAE). O titular não pode ser ele mesmo um substituto. O substituto **herda o nível** do titular: não informe outro |
| `recebe_copia_pae` | não | `true` se a entidade recebe cópia do PAE (§12) |

**A ordem da lista é a ordem de acionamento dentro de cada nível.** Entre níveis, o Desktop ordena do verde
para o vermelho.

Ao montar a lista de acionamento, o Desktop aponta como **pendência** o papel que o PAE prevê no nível e que
não tem contato: coordenador do PAE e empreendedor (nível 1), entidade fiscalizadora (2) e Defesa Civil (3).
A Central deve alertar sobre essas faltas ao publicar, sem impedir a publicação (há barragens sem PAE).

> Contatos são dados pessoais (LGPD): publique só o necessário ao PAE. Os exemplos deste repositório usam
> nomes e números fictícios.

## Comportamento no Desktop

| Situação | O que acontece |
|---|---|
| Barragem nova na lista | Cadastrada |
| `versao` diferente da guardada | Dados atualizados; grupos, campos e contatos novos são incluídos, alterados são atualizados e os que saíram recebem exclusão lógica |
| Mesma `versao` | Nada muda |
| Barragem que estava excluída volta à lista | Reativada |
| Barragem saiu da lista | **Exclusão lógica**: some das telas e deixa de aceitar medições; os dados continuam gravados |
| Cadastro inválido (obrigatório ausente, valor fora da regra, campo num grupo que não existe, contato acionado sem meio de contato, substituto de um contato que não existe) | **Recusado**: a cópia anterior dessa barragem é mantida e o erro vai para o log |
| Arquivo ausente, JSON ilegível, sem `barragens` ou nenhum cadastro válido | **Nada é alterado** |

Na Central, aplique as mesmas regras na validação dos formulários, para que um cadastro inválido nunca seja
publicado.

## Em aberto

- Lista oficial de campos do cadastro estadual (SEMAD, IN 01/2020) e do SNISB (ANA): quando disponível, entra
  no catálogo, e os campos exigidos podem virar obrigatórios.
- Estruturas associadas e ZAS: entram como listas próprias do cadastro numa próxima versão deste contrato.
- O mapeamento entre severidade e nível de resposta (OK 0, AVISO 1 verde, ALERTA 2 amarelo, CRITICO 3
  vermelho) é do motor e ainda será validado com o professor. O PAE do João Leite junta o laranja da ANA ao
  amarelo; um PAE com quatro níveis precisaria de outro mapeamento.
