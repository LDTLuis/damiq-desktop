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
      "campos": [
        { "chave": "cota_crista", "rotulo": "Cota da crista", "grupo": "Maciço",
          "tipo": "NUMERO", "valor": 752.5, "unidade": "m" }
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
| `campos` | lista | Campos próprios da barragem (abaixo) |

### Campos próprios (`campos`)

Cada barragem tem as características e indicadores que fizerem sentido para ela, além dos obrigatórios
(ex.: os da ficha técnica do PAE: crista, vertedouro, volumes, hidrologia, regularização).

| Campo | Obrigatório | Regra |
|---|---|---|
| `chave` | sim | Identificador estável do campo na barragem: minúsculas, números e `_`, começando por letra, até 64 caracteres. Única na barragem |
| `rotulo` | sim | Nome exibido (ex.: "Cota da crista") |
| `grupo` | não | Agrupamento na tela (ex.: "Maciço", "Vertedouro", "Reservatório") |
| `tipo` | sim | `NUMERO`, `TEXTO`, `DATA` ou `BOOLEANO` |
| `valor` | sim | `NUMERO`: número com ponto decimal; `DATA`: `AAAA-MM-DD`; `BOOLEANO`: `true`/`false`; `TEXTO`: livre |
| `unidade` | não | Ex.: `m`, `hm³`, `m³/s`, `km²` |

A ordem da lista é a ordem de exibição.

## Comportamento no Desktop

| Situação | O que acontece |
|---|---|
| Barragem nova na lista | Cadastrada |
| `versao` diferente da guardada | Dados atualizados; campos próprios novos são incluídos, alterados são atualizados e os que saíram recebem exclusão lógica |
| Mesma `versao` | Nada muda |
| Barragem que estava excluída volta à lista | Reativada |
| Barragem saiu da lista | **Exclusão lógica**: some das telas e deixa de aceitar medições; os dados continuam gravados |
| Cadastro inválido (obrigatório ausente, valor fora da regra) | **Recusado**: a cópia anterior dessa barragem é mantida e o erro vai para o log |
| Arquivo ausente, JSON ilegível, sem `barragens` ou nenhum cadastro válido | **Nada é alterado** |

Na Central, aplique as mesmas regras na validação dos formulários, para que um cadastro inválido nunca seja
publicado.

## Em aberto

- Lista oficial de campos do cadastro estadual (SEMAD, IN 01/2020) e do SNISB (ANA): quando disponível, os
  campos exigidos que hoje vão em `campos` podem virar obrigatórios.
- Contatos e ordem de acionamento do PAE, estruturas associadas e ZAS: entram como listas próprias do
  cadastro numa próxima versão deste contrato.
