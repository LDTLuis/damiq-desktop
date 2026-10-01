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

## Comportamento no Desktop

| Situação | O que acontece |
|---|---|
| Barragem nova na lista | Cadastrada |
| `versao` diferente da guardada | Dados atualizados; grupos e campos novos são incluídos, alterados são atualizados e os que saíram recebem exclusão lógica |
| Mesma `versao` | Nada muda |
| Barragem que estava excluída volta à lista | Reativada |
| Barragem saiu da lista | **Exclusão lógica**: some das telas e deixa de aceitar medições; os dados continuam gravados |
| Cadastro inválido (obrigatório ausente, valor fora da regra, campo num grupo que não existe) | **Recusado**: a cópia anterior dessa barragem é mantida e o erro vai para o log |
| Arquivo ausente, JSON ilegível, sem `barragens` ou nenhum cadastro válido | **Nada é alterado** |

Na Central, aplique as mesmas regras na validação dos formulários, para que um cadastro inválido nunca seja
publicado.

## Em aberto

- Lista oficial de campos do cadastro estadual (SEMAD, IN 01/2020) e do SNISB (ANA): quando disponível, entra
  no catálogo, e os campos exigidos podem virar obrigatórios.
- Contatos e ordem de acionamento do PAE, estruturas associadas e ZAS: entram como listas próprias do
  cadastro numa próxima versão deste contrato.
