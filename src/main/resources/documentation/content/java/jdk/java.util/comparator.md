---
id: java/jdk/java.util/comparator
title: Comparator
type: api
summary: Comparadores para ordenar objetos no Java, com comparing, thenComparing, reverseOrder e as armadilhas de overflow e de contrato com equals.
level: beginner
duration: 8
officialDocs:
  label: API java.util.Comparator
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Comparator.html
related:
  - java/jdk/java.util/collections
  - java/jdk/java.util/stream
  - java/jdk/java.util/list
---

> [!INFO] `Comparator` é uma função que diz quem vem **antes** de quem: negativo, zero ou positivo. Em vez de escrever `a.getIdade() - b.getIdade()`, use `Comparator.comparing(Pessoa::getIdade)` — legível, sem overflow e com `thenComparing` para desempate.

## Visão geral

Ordem natural (por `Comparable`) cobre `String`, `Integer`, `LocalDate`... Mas ordenar pessoas por sobrenome, produtos por preço decrescente ou registros por dois critérios exige um comparador — a função passada a `sort`, `TreeSet`, `TreeMap` e `Stream.sorted`.

```java
List<String> nomes = new ArrayList<>(List.of("bia", "ana"));
nomes.sort(Comparator.naturalOrder());                 // [ana, bia]
nomes.sort(Comparator.comparingInt(String::length));   // pelo tamanho
nomes.sort(Comparator.reverseOrder());                 // ordem inversa
```

`Comparator` é uma *functional interface* — um único método abstrato `compare`, então lambdas funcionam: `(a, b) -> a.length() - b.length()` (mas veja a armadilha de overflow).

## Assinaturas

| Método | Retorna | O que faz |
|--------|---------|-----------|
| `compare(T a, T b)` (abstrato) | `int` | negativo se `a < b`, 0 se igual, positivo se `a > b` |
| `naturalOrder()` (estático) | `Comparator<T>` | ordem natural (T precisa de `Comparable`) |
| `reverseOrder()` (estático) | `Comparator<T>` | ordem natural invertida |
| `comparing(Function)` (estático) | `Comparator<T>` | ordena por um campo extraído |
| `comparing(Function, Comparator)` (estático) | `Comparator<T>` | extrai e compara com comparador próprio |
| `comparingInt/Long/Double(...)` (estático) | `Comparator<T>` | por campo primitivo (sem boxing) |
| `nullsFirst(Comparator)` / `nullsLast(Comparator)` (estático) | `Comparator<T>` | trata `null` no começo/fim |
| `reversed()` (padrão) | `Comparator<T>` | inverte a ordem |
| `thenComparing(Comparator)` / `thenComparing(Function)` (padrão) | `Comparator<T>` | critério de desempate |
| `thenComparingInt/Long/Double(...)` (padrão) | `Comparator<T>` | desempate por primitivo |
| `equals(Object)` (padrão) | `boolean` | igualdade com outro comparador |

## Ordenar por um campo

```java
record Pessoa(String nome, int idade) {}

List<Pessoa> pessoas = new ArrayList<>(
        List.of(new Pessoa("Ana", 30), new Pessoa("Bia", 25), new Pessoa("Caio", 30)));

pessoas.sort(Comparator.comparing(Pessoa::nome));
System.out.println(pessoas);   // [Pessoa[nome=Ana...], Pessoa[nome=Bia...], Pessoa[nome=Caio...]]

pessoas.sort(Comparator.comparingInt(Pessoa::idade));
// idade 25 (Bia) primeiro — as de 30 mantêm a ordem relativa (sort estável)

pessoas.sort(Comparator.comparing(p -> p.nome().length()));  // lambda também vale
```

**`comparing` extrai a chave e compara com a ordem natural dela** — quem extrai precisa devolver algo comparável (`String`, `Integer`, `LocalDate`...).

## Encadear critérios

```java
record Produto(String categoria, String nome, double preco) {}
List<Produto> loja = new ArrayList<>(List.of(
        new Produto("livro", "Zebra", 50),
        new Produto("livro", "Ana", 50),
        new Produto("brinquedo", "Bola", 30)));

Comparator<Produto> ordem = Comparator
        .comparing(Produto::categoria)
        .thenComparing(Produto::nome)
        .thenComparing(Comparator.comparingDouble(Produto::preco).reversed());

loja.sort(ordem);
System.out.println(loja.get(0).categoria());   // brinquedo (primeira chave)
System.out.println(loja.get(1).nome());        // Ana (desempate alfabético em "livro")
```

`thenComparing` só roda quando o critério anterior devolveu `0` — é o desempate encadeado.

**Armadilha:** a **ordem dos critérios muda o resultado** — trocar `categoria` com `nome` produz outra lista, mesmo com os mesmos métodos.

## Ordem inversa, caso e null

```java
List<Integer> nums = new ArrayList<>(List.of(3, 1, 2));
nums.sort(Comparator.naturalOrder().reversed());
System.out.println(nums);                       // [3, 2, 1]

List<String> cores = new ArrayList<>(List.of("azul", "Azul", "amarelo"));
cores.sort(String.CASE_INSENSITIVE_ORDER);
System.out.println(cores);                      // [amarelo, azul, Azul] — sem chocar A/a

List<String> comNulo = new ArrayList<>(Arrays.asList("b", null, "a"));
comNulo.sort(Comparator.nullsLast(Comparator.naturalOrder()));
System.out.println(comNulo);                    // [a, b, null]
```

**Armadilha:** `naturalOrder()` lança `NullPointerException` se houver `null` — ordene com `nullsFirst`/`nullsLast` quando a ausência é possível.

## Armadilhas comuns

> [!WARNING] Comparar com subtração (`a.getIdade() - b.getIdade()`) **estoura em overflow** com inteiros extremos e devolve sinal errado. Use `Integer.compare(a, b)` ou `Comparator.comparingInt(...)` — o sinal nunca mente.

**Comparador inconsistente com `equals`:**

```java
Comparator<Pessoa> porIdade = Comparator.comparingInt(p -> p.idade());
Pessoa a = new Pessoa("Ana", 30);
Pessoa b = new Pessoa("Bia", 30);
porIdade.compare(a, b);    // 0 — considera "iguais"
a.equals(b);               // false — mas equals diz que são diferentes

// em TreeSet/TreeMap isso é sério: elementos com compare == 0 são DESCARTADOS
Set<Pessoa> conjunto = new TreeSet<>(porIdade);
```

**Ordenar `String` sem considerar caixa:**

```java
List<String> nomes = new ArrayList<>(List.of("Zebra", "apple"));
nomes.sort(Comparator.naturalOrder());            // [Zebra, apple] — 'Z'(90) < 'a'(97)
nomes.sort(String.CASE_INSENSITIVE_ORDER);        // [apple, Zebra] — caixa deixa de importar
```

**Esquecer que `sort` altera a lista:**

```java
List<Integer> fixa = List.of(2, 1);
fixa.sort(null);                // UnsupportedOperationException — List.of é imutável
new ArrayList<>(fixa).sort(null);   // cópia mutável, ordena de verdade
```

## Profundidade

**Contrato (API de `Comparator`):** `sgn(compare(a,b)) == -sgn(compare(b,a))`, transitividade (`a<b` e `b<c` ⇒ `a<c`) e consistência com o próprio estado. Violar esses itens deixa `sort` em comportamento indefinido (TimSort lança `IllegalArgumentException: Comparison method violates its general contract`).

**Ordenação estável:** o `sort` do Java usa TimSort, que **preserva a ordem relativa** de elementos empatados — por isso "ordenar por nome e depois por idade" funciona sem escrever o desempate, e por isso ordenar por um campo só mantém o restante como estava.

**Primitivos e `comparingInt`:** `comparing(p -> p.idade())` autoboxeia `int` → `Integer` para cada comparação; `comparingInt` evita o boxing e o `Integer.compare` implícito (menos alocação em listas grandes).

**`Comparable` vs `Comparator`:** `Comparable` é a ordem natural *dentro* da classe (`compareTo`); `Comparator` é ordem *externa*, você pode ter vários para a mesma classe (por nome, por preço, inverso). Em classes de domínio é comum deixar sem `Comparable` e deixar quem ordena escolher o `Comparator`.

**Desempenho:** `compare` é chamado O(n log n) vezes — mantenha-o barato: extraia a chave e compare, sem acesso a banco, sem cálculo pesado, sem efeitos colaterais.
