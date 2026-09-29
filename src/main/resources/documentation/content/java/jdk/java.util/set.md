---
id: java/jdk/java.util/set
title: Set
type: api
summary: Conjunto sem duplicatas no Java, comparando HashSet, LinkedHashSet e TreeSet, com os métodos essenciais e as armadilhas do contrato equals e hashCode.
level: beginner
duration: 7
officialDocs:
  label: API java.util.Set
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Set.html
related:
  - java/jdk/java.util/list
  - java/jdk/java.util/map
  - java/jdk/java.util/collections
---

> [!INFO] `Set` é uma coleção **sem duplicatas**: adicionar um valor que já existe devolve `false` e não muda nada. Escolha `HashSet` para velocidade, `LinkedHashSet` para manter a ordem de inserção e `TreeSet` para manter ordenado.

## Visão geral

Um conjunto responde "esse elemento já está aqui?" de forma barata. É a ferramenta natural para **remover duplicatas**, testar pertinência (`contains` em O(1) médio) e operar em matemática de conjuntos (união, interseção, diferença).

```java
Set<String> tags = new HashSet<>();          // mais rápido, sem ordem
Set<Integer> ids = new LinkedHashSet<>();    // ordem de inserção
Set<String> alfabeto = new TreeSet<>();       // sempre ordenado (natural ou Comparator)
Set<String> fixo = Set.of("java", "kotlin"); // imutável, tamanho fixo
```

A interface é a mesma; muda só o custo e a ordem de iteração.

## Assinaturas

| Método | Retorna | O que faz |
|--------|---------|-----------|
| `add(E e)` | `boolean` | adiciona; `false` se já existia |
| `remove(Object o)` | `boolean` | remove o valor; `false` se não existia |
| `contains(Object o)` | `boolean` | se o valor pertence ao conjunto |
| `size()` | `int` | quantidade de elementos |
| `isEmpty()` | `boolean` | se está vazio |
| `clear()` | `void` | esvazia |
| `addAll(Colecao)` | `boolean` | união (em lugar) |
| `retainAll(Colecao)` | `boolean` | interseção (em lugar) |
| `removeAll(Colecao)` | `boolean` | diferença (em lugar) |
| `containsAll(Colecao)` | `boolean` | se contém todos |
| `forEach(Consumer)` | `void` | percorre com lambda |
| `toArray()` | `Object[]` | copia para array |
| `Set.of(E...)` (estático) | `Set<E>` | conjunto imutável |

## Adicionar e remover

```java
Set<String> usuarios = new HashSet<>();
usuarios.add("ana");     // true  -> [ana]
usuarios.add("bia");     // true  -> [ana, bia]
usuarios.add("ana");     // false — já existe, nada muda
usuarios.size();         // 2 (nunca 3)
usuarios.remove("bia");  // true
usuarios.contains("ana");// true
// add devolve false em vez de estourar exceção: o valor simplesmente passa a valer
```

**Armadilha:** `remove` de valor ausente devolve `false` e **não** lança — confie no retorno, não espere exceção.

## Consultar

```java
Set<String> tags = new HashSet<>(List.of("java", "backend", "java"));
tags.size();                        // 2 — a duplicata "java" foi descartada
tags.contains("backend");           // true
tags.contains("frontend");          // false

List<String> originais = List.of("a", "b", "a", "c");
Set<String> unicos = new HashSet<>(originais);   // jeito curto de eliminar duplicatas
System.out.println(unicos.size());  // 3
```

Armadilha: `contains` usa `equals` **e** `hashCode` do objeto — sem esses dois métodos, o `HashSet` não encontra nem remove nada (ver Armadilhas).

## União, interseção e diferença

```java
Set<Integer> a = new HashSet<>(List.of(1, 2, 3));
Set<Integer> b = new HashSet<>(List.of(3, 4));

Set<Integer> uniao = new HashSet<>(a);
uniao.addAll(b);            // {1, 2, 3, 4}

Set<Integer> inter = new HashSet<>(a);
inter.retainAll(b);         // {3}

Set<Integer> dif = new HashSet<>(a);
dif.removeAll(b);           // {1, 2}
System.out.println(uniao + " | " + inter + " | " + dif);
// [1, 2, 3, 4] | [3] | [1, 2]
```

**As operações modificam o próprio conjunto** — por isso começamos sempre a partir de uma cópia (`new HashSet<>(a)`).

## Armadilhas comuns

> [!WARNING] Objetos sem `equals` e `hashCode` viram "fantasmas" no `HashSet`: `add` aceita duplicatas iguais e `contains`/`remove` devolvem `false`. Implemente os dois métodos juntos (ou use registros/enum).

**Ordem de iteração é imprevisível em `HashSet`:**

```java
Set<String> s = new HashSet<>(List.of("b", "a", "c"));
for (String x : s) System.out.print(x + " ");   // qualquer ordem (ex.: a c b)
Set<String> ordenado = new TreeSet<>(s);        // [a, b, c] — TreeSet resolve
```

**`TreeSet` rejeita `null` e exige compatibilidade:**

```java
Set<String> t = new TreeSet<>();
t.add(null);                       // NullPointerException (ordem natural não compara null)
Set<Object> mistura = new TreeSet<>();
mistura.add(1);
mistura.add("x");                  // ClassCastException: Integer e String não se comparam
```

**`Set.of` é imutável:** `Set.of("a").add("b")` lança `UnsupportedOperationException`. Para mudar depois, use `new HashSet<>()`.

## Profundidade

**`hashCode` e buckets:** o `HashSet` (por trás, um `HashMap`) calcula `hashCode` do elemento, encontra o balde e compara com `equals`. Em O(1) médio; colisões de hash pioram para O(n) no balde. `LinkedHashSet` acrescenta uma lista encadeada dupla para lembrar a ordem de inserção.

**Árvore rubro-negra:** `TreeSet` guarda os elementos em árvore balanceada — `add`/`remove`/`contains` são O(log n) e a iteração sai **ordenada**. Como é `NavigableSet`, também oferece `floor`, `ceiling`, `headSet`, `tailSet`.

**Contrato de igualdade (Set):** dois sets são iguais se contêm os mesmos elementos (independente da ordem) — `equals` de `Set` compara tamanhos e `containsAll`. Isso é o que permite `new HashSet<>(list).equals(new HashSet<>(outra))` como teste de "mesmos elementos".

**Comparador em `TreeSet`:** `new TreeSet<>(Comparator.comparing(Pessoa::idade))` ordena pela idade — e **descarta** elementos que o comparador considera iguais (`compare == 0`), mesmo que não sejam `equals`. Nunca misture naturalOrder com Comparator depois de popular.

**`contains` vs `indexOf`:** em `Set` não existe índice nem posição; qualquer acesso é por valor. Se você precisa de posição, quer uma `List`.
