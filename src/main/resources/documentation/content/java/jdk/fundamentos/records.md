---
id: java/jdk/fundamentos/records
title: Records
type: concept
summary: Portadores de dados imutáveis em uma linha — equals, hashCode e toString automáticos, construtor compacto para validação e os limites do que um record aceita.
level: beginner
duration: 7
officialDocs:
  label: JLS §8.10 — Record Classes
  url: https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.10
related:
  - java/jdk/fundamentos/classes
  - java/jdk/fundamentos/interfaces
  - java/jdk/java.lang/object
---

> [!INFO] `record` declara uma classe cuja missão é **carregar dados**: você lista os campos na própria assinatura e o compilador gera construtor, acessadores, `equals`, `hashCode` e `toString`. Os campos são finais — um record é **imutável por design**.

## Por que existe

Toda classe de dados nasce igual: campos, construtor que só copia parâmetros, um getter para cada campo, `equals`/`hashCode` comparando um a um, `toString` para depurar. Dezenas de linhas de cerimônia para uma linha de intenção — e um risco clássico: ninguém lembra de escrever o `equals` e o objeto nunca funciona como chave de `HashMap`.

O record troca boilerplate por **semântica**: `record Dinheiro(double valor, String moeda)` diz o que importa (os dados) e promete o resto — imutável, comparável por valor, pronto para imprimir. Desde o Java 16 é a forma idiomática de agrupar valores, devolver múltiplos resultados e criar DTOs.

## Anatomia da sintaxe

```java
public record Ponto(int x, int y) { }   // x e y: componentes do estado
```

O que o compilador gera a partir dessa única linha:

| Membro gerado | Assinatura | O que faz |
|---------------|------------|-----------|
| construtor canônico | `Ponto(int x, int y)` | recebe e grava cada componente |
| acessadores | `x()` e `y()` | leem o campo — **sem** prefixo `get` |
| `equals` / `hashCode` | — | comparam componente a componente |
| `toString` | — | `Ponto[x=3, y=4]` |

O corpo `{ }` é opcional e fica para o que o compilador não sabe fazer — validação e métodos extras:

```java
record Cpf(String valor) {
    Cpf {                                   // construtor compacto: sem parênteses
        if (valor == null) throw new IllegalArgumentException("valor obrigatorio");
        valor = valor.strip();              // ajusta o parâmetro antes de gravar
    }

    boolean valido() { return valor.length() == 11; }   // comportamento extra
}
```

## Como funciona

O cabeçalho **é** o estado: cada componente vira um campo `private final` no record. O `equals` gerado compara os componentes com `equals` deles — dois `new Ponto(3, 4)` são iguais de verdade — e o `hashCode` acompanha: mesmos componentes, mesmo hash. É isso que faz o record funcionar como chave de `HashMap` sem você escrever nada.

Por ser imutável e comparável por valor, o record é **transparente**: a identidade (o endereço) perde importância, só os dados importam. Sob o capô, o compilador gera uma classe `final` que estende `java.lang.Record` — não pode ser estendida, mas pode implementar interfaces.

## Exemplos

Criar, ler e comparar — sem nenhum método escrito à mão:

```java
record Dinheiro(double valor, String moeda) { }

Dinheiro preco = new Dinheiro(19.9, "BRL");
preco.valor();                              // 19.9 — acessor sem get
System.out.println(preco);                  // Dinheiro[valor=19.9, moeda=BRL]

preco.equals(new Dinheiro(19.9, "BRL"));    // true — valor por valor
```

Como chave de mapa — o caso que mais sofre com `equals`/`hashCode` manual:

```java
record CodigoPedido(String valor) { }

Map<CodigoPedido, String> pedidos = new HashMap<>();
pedidos.put(new CodigoPedido("A-100"), "enviado");

pedidos.get(new CodigoPedido("A-100"));    // "enviado" — objeto novo, mesmo valor
```

Validação + interface — o pacote completo:

```java
record Intervalo(int inicio, int fim) implements Comparable<Intervalo> {
    Intervalo {
        if (fim < inicio) throw new IllegalArgumentException("fim < inicio");
    }
    public int compareTo(Intervalo outro) {
        return Integer.compare(inicio, outro.inicio);
    }
}

new Intervalo(10, 2);   // IllegalArgumentException na criação: estado inválido não existe
```

## Armadilhas

> [!WARNING] A imutabilidade é **rasa**: o record guarda a referência do componente. `List`, array e objetos mutáveis continuam mutáveis por fora — para imutabilidade de verdade, faça cópia defensiva no construtor compacto.

**Componente mutável escapando — e a defesa:**

```java
record Turma(List<String> alunos) {
    Turma { alunos = List.copyOf(alunos); }   // cópia defensiva
}

List<String> nomes = new ArrayList<>(List.of("ana"));
Turma t = new Turma(nomes);
nomes.add("bia");                             // não muda nada dentro de t
```

**Array como componente:** o `equals` gerado compara arrays por **referência** — dois arrays com o mesmo conteúdo dão `false`. Prefira `List`, ou escreva os métodos por cima.

**Acessor sem `get`:** `preco.getValor()` não existe; é `preco.valor()`. Quem procura o getter no padrão JavaBean não encontra nada.

**Record não é entidade mutável:** frameworks que criam proxies e trocam estado (como entidades JPA) não combinam com uma classe final e imutável. Para portador de dados puro, record; para objeto com ciclo de vida mutável, classe comum.

## Profundidade

**JLS §8.10:** record é classe `final` que estende `java.lang.Record`; os componentes viram campos `private final`, e os membros correspondentes de `equals`, `hashCode`, `toString` e dos acessadores são gerados pelo compilador. Sobrescrever qualquer um é permitido — e raramente necessário.

**Construtor compacto:** declarado com o nome do record e **sem lista de parâmetros**, roda no início do construtor canônico, antes dos campos serem gravados. Reatribuir o parâmetro (`valor = valor.strip()`) muda o que será gravado; `throw` interrompe a criação.

**Record patterns (Java 21):** records destruturam em `instanceof` e `switch` — `if (obj instanceof Ponto(int x, int y))` extrai os componentes direto para variáveis.

**Quando escolher record vs classe:** se a resposta for "carregar um grupo de valores de um lado para o outro" — retorno múltiplo, chave de mapa, DTO —, record. Se houver identidade mutável, invariantes complexos ou herança, classe comum.
