---
id: java/jdk/java.lang/comparable
title: Comparable
type: api
summary: A interface da ordem natural dos objetos — compareTo, a relação com equals e como coleções ordenadas usam essa ordem.
level: beginner
duration: 7
officialDocs:
  label: API java.lang.Comparable
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Comparable.html
related:
  - java/jdk/java.util/collections
  - java/jdk/java.util/set
---

> [!INFO] `Comparable<T>` define a **ordem natural** de uma classe: `compareTo` devolve negativo, zero ou positivo. É o que faz `sort`, `TreeSet` e `Collections.max` ordenarem seus objetos — e o contrato exige que ele seja **consistente com `equals`**.

## Visão geral

Ordenar é responder "quem vem antes?". Em vez de a linguagem saber ordenar qualquer coisa, Java pede que a própria classe responda isso implementando uma interface com um único método:

```java
record Produto(String nome, double preco) implements Comparable<Produto> {
    @Override public int compareTo(Produto outro) {
        return Double.compare(preco, outro.preco);   // ordem por preço
    }
}
```

Classes prontas já vêm ordenadas (`String`, `Integer`, `LocalDate`, enum). Para as suas, `implements Comparable<T>` habilita toda a família de APIs de ordenação sem escrever mais nada.

## Assinaturas

| Método | Retorna | O que faz |
|--------|---------|-----------|
| `compareTo(T outro)` | `int` | `< 0` antes, `0` empate, `> 0` depois |
| `Integer.compare(int, int)` | `int` | compara `int` sem erro de truncamento |
| `Double.compare(double, double)` | `int` | compara `double` (cuida de NaN e -0.0) |
| `String.compareTo(String)` | `int` | ordem lexicográfica da String |
| `Boolean.compare(boolean, boolean)` | `int` | `false` antes de `true` |
| `list.sort(null)` / `Collections.sort(list)` | `void` | ordena pela ordem natural |
| `Comparator.naturalOrder()` | `Comparator<T>` | ordem natural como `Comparator` |
| `Collections.max(col)` / `min(col)` | `T` | extremos pela ordem natural |
| `new TreeSet<>(col)` | `TreeSet<T>` | conjunto ordenado pela ordem natural |
| `stream.sorted()` | `Stream<T>` | ordena o stream pela ordem natural |
| `Arrays.sort(vetor)` | `void` | ordena array de objetos pela ordem natural |

## Definindo a ordem natural

```java
record Aluno(String nome, double media) implements Comparable<Aluno> {
    @Override
    public int compareTo(Aluno outro) {
        return Double.compare(this.media, outro.media);   // menor média primeiro
    }
}

Aluno a = new Aluno("Ana", 8.5);
Aluno b = new Aluno("Bia", 9.1);
a.compareTo(b);          // negativo — Ana vem antes de Bia
b.compareTo(a);          // positivo — simetria invertida
a.compareTo(a);          // 0 — empate
```

**`compareTo` fala de ordem, não de igualdade**: o zero significa "empate na ordenação" e nada garante que os objetos sejam `equals`. Igualdade real continua sendo `equals`.

## Ordenando com a ordem natural

```java
List<Aluno> turma = new ArrayList<>(List.of(
        new Aluno("Ana", 8.5), new Aluno("Bia", 9.1), new Aluno("Caio", 7.0)));

turma.sort(null);                       // ordem natural (usa compareTo)
Collections.sort(turma);                // idem — legado
Collections.max(turma).nome();          // "Bia" — maior média

List<String> nomes = Stream.of("Bia", "Ana", "Caio").sorted().toList();
// [Ana, Bia, Caio]
TreeSet<Integer> nums = new TreeSet<>(List.of(3, 1, 2));   // [1, 2, 3]
```

**Se a classe não implementa `Comparable`, `sort(null)` estoura `ClassCastException`** — a ordem natural não existe e a API não adivinha critério por você.

## Critérios externos com Comparator

```java
List<Aluno> turma = new ArrayList<>(List.of(
        new Aluno("Ana", 8.5), new Aluno("Bia", 9.1), new Aluno("Caio", 7.0)));

// ordem decrescente — trocar os argumentos inverte a ordem
turma.sort(Comparator.comparingDouble(Aluno::media).reversed());

// empate por média → desempata pelo nome
turma.sort(Comparator.comparingDouble(Aluno::media).thenComparing(Aluno::nome));

Collections.reverse(turma);             // inverte a ordem atual
```

**Para ordem decrescente, troque os argumentos** (`Double.compare(outro, this)` ou `.reversed()`) — negar o resultado com `-` parece igual, mas é a operação que mais escapa em casos extremos.

## Armadilhas comuns

> [!WARNING] `compareTo` inconsistente com `equals`: se `a.compareTo(b) == 0` mas `a.equals(b) == false`, `TreeSet`/`TreeMap` tratam como o mesmo item e **descartam** um — porque decidem igualdade pelo `compareTo`, não pelo `equals`.

```java
TreeSet<Aluno> set = new TreeSet<>();   // ordena só por média
set.add(new Aluno("Ana", 8.0));
set.add(new Aluno("Ana", 8.0));   // empate em compareTo → ignorado
set.size();                       // 1 — o segundo "desapareceu"
```

Se o conjunto precisa guardar objetos distintos com mesma ordem, use `HashSet` (igualdade por `equals`) ou inclua um desempate no `compareTo`.

**Subtração truncada no retorno:**

```java
// ruim — 0.5 - 0.4 = 0.099... truncado vira 0 ("empate" indevido)
public int compareTo(Produto p) { return (int) (this.preco - p.preco); }

// correto
public int compareTo(Produto p) { return Double.compare(this.preco, p.preco); }
```

Além do truncamento, `int - int` estoura: `Integer.MIN_VALUE - 1` vira positivo e inverte a ordem.

**Ordenar com `null` na lista:**

```java
List<String> nomes = new ArrayList<>(Arrays.asList("Ana", null, "Bia"));
nomes.sort(null);                          // NullPointerException
nomes.sort(Comparator.nullsLast(Comparator.naturalOrder()));   // null no fim
```

A ordem natural não sabe onde colocar `null` — trate antes ou use os auxiliares `nullsFirst`/`nullsLast`.

## Profundidade

**Contrato de `compareTo` (API de Comparable):** o sinal precisa ser antisimétrico (`sgn(x.compareTo(y)) == -sgn(y.compareTo(x))`), transitivo e consistente entre chamadas; e, para classes que não são abstratas, `(x.compareTo(y) == 0)` deve coincidir com `x.equals(y)`. A quebra mais comum é exatamente o terceiro: ordenar por um campo e comparar por todos.

**Por que não `a - b`:** subtrair dois valores e truncar perde a fração (empates falsos) e sofre estouro de faixa. O padrão seguro é sempre `T.compareTo`/`T.compare` do próprio tipo (`Integer.compare`, `Double.compare`, `LocalDate.compareTo`).

**`Comparable` vs `Comparator`:** `Comparable` fica **dentro** da classe e define *uma* ordem natural (a única); `Comparator` fica **fora** e cria quantas ordens forem necessárias, além de ser combinável (`reversed()`, `thenComparing`, `comparingDouble`). A recomendação clássica é preferir a ordem natural quando ela for óbvia e só criar `Comparator` para os casos especiais.

**Enum e record:** enum **já é** `Comparable` (ordena pela ordem de declaração, via `ordinal`) — não sobrescreva. Record **não** implementa sozinho; declare `implements Comparable<Ponto>` e escolha os componentes que definem a ordem, que pode ser diferente da ordem dos componentes no construtor.

**`TreeSet`/`TreeMap` usam ordem total:** eles precisam decidir igualdade por comparação, por isso `compareTo == 0` equivale a "mesmo elemento". `HashSet`/`HashMap` usam `hashCode` + `equals` — as duas famílias não conversam: o mesmo objeto pode ser único num `HashSet` e "duplicado" num `TreeSet`.

**Ordenação estável:** `List.sort` e `Arrays.sort` de objetos usam TimSort, que é **estável** — itens empatados mantêm a ordem relativa da entrada. Isso permite ordenações encadeadas (`thenComparing` ou sorts sucessivos) sem perder o critério anterior.
