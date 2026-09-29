---
id: java/jdk/java.util/list
title: List
type: api
summary: Lista ordenada e indexada do Java, com ArrayList na prática, métodos do dia a dia e as armadilhas de remove por índice e por valor.
level: beginner
duration: 8
officialDocs:
  label: API java.util.List
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/List.html
related:
  - java/jdk/java.util/set
  - java/jdk/java.util/stream
  - java/jdk/java.util/collections
---

> [!INFO] `List` é a coleção **ordenada e indexada** que aceita elementos **repetidos** — a posição de inserção é preservada. No dia a dia, use `ArrayList` e sempre declare pela interface: `List<String> lista = new ArrayList<>();`.

## Visão geral

`List` modela uma sequência: cada elemento tem uma posição (`0` a `size() - 1`), dá para acessar por índice, inserir no meio e ter o mesmo valor duas vezes. É a escolha certa quando a **ordem importa** — filas de exibição, histórico, resultados de consulta.

```java
List<String> nomes = new ArrayList<>();   // implementação usual: array por dentro
List<Integer> nums = List.of(1, 2, 3);    // imutável, tamanho fixo
List<String> ligada = new LinkedList<>(); // só se houver muita remoção no meio
```

`ArrayList` é rápido para ler por índice e para anexar no fim; `LinkedList` só compensa quando você insere/remove constantemente no meio (fora disso, perde para `ArrayList`).

## Assinaturas

| Método | Retorna | O que faz |
|--------|---------|-----------|
| `size()` | `int` | quantidade de elementos |
| `isEmpty()` | `boolean` | se a lista está vazia |
| `get(int i)` | `E` | elemento na posição `i` |
| `set(int i, E e)` | `E` | substitui e devolve o antigo |
| `add(E e)` | `boolean` | anexa no fim |
| `add(int i, E e)` | `void` | insere na posição `i` |
| `remove(int i)` | `E` | remove **pela posição** |
| `remove(Object o)` | `boolean` | remove a 1ª ocorrência do **valor** |
| `indexOf(Object o)` | `int` | posição do valor ou `-1` |
| `contains(Object o)` | `boolean` | se o valor está presente |
| `clear()` | `void` | esvazia a lista |
| `sort(Comparator<? super E>)` | `void` | ordena no lugar |
| `forEach(Consumer<? super E>)` | `void` | percorre com lambda |
| `toArray()` | `Object[]` | copia para array |
| `List.of(E...)` (estático) | `List<E>` | lista imutável |

## Adicionar e remover

```java
List<String> tarefas = new ArrayList<>();
tarefas.add("escrever");              // [escrever]
tarefas.add("revisar");               // [escrever, revisar]
tarefas.add(0, "abrir");              // [abrir, escrever, revisar]
tarefas.remove(1);                    // remove por índice -> [abrir, revisar]
tarefas.remove("abrir");              // remove por valor  -> [revisar]
tarefas.size();                       // 1
tarefas.isEmpty();                    // false
// Índices vão de 0 a size()-1 — get(1) numa lista de 1 item dá IndexOutOfBoundsException
```

**`remove(int)` tira pela posição e `remove(Object)` pelo valor** — a sobrecarga escolhida depende do tipo do argumento.

## Consultar e percorrer

```java
List<Integer> nums = List.of(4, 8, 15, 16, 23);
nums.get(2);                 // 15
nums.contains(16);           // true
nums.indexOf(99);            // -1 (não existe)
nums.forEach(n -> System.out.print(n + " "));   // 4 8 15 16 23

for (int i = 0; i < nums.size(); i++) {
    System.out.println(nums.get(i));            // acesso por índice, O(1) no ArrayList
}
```

**Armadilha:** `for-each` não dá o índice; se precisar da posição, percorra por `for` tradicional ou use `list.sort` antes de buscar com `binarySearch` (ver `Arrays`).

## Ordenar

```java
List<String> times = new ArrayList<>(List.of("Flamengo", "Bahia", "Atlético"));
times.sort(Comparator.naturalOrder());       // ordem alfabética
System.out.println(times);                   // [Atlético, Bahia, Flamengo]

times.sort(Comparator.comparing(String::length));  // pelo tamanho
System.out.println(times);                   // [Bahia, Atlético, Flamengo]

List<Integer> nums = new ArrayList<>(List.of(3, 1, 2));
Collections.sort(nums);                      // idem, versão estática
System.out.println(nums);                    // [1, 2, 3]
```

**`sort` ordena no lugar e devolve `void`** — quem espera `List` de volta (`List<Integer> ok = nums.sort(...)`) não compila.

## Armadilhas comuns

> [!WARNING] `remove(int)` e `remove(Object)` são ambíguos em listas genéricas: `lista.remove(1)` sempre remove o **elemento de índice 1**. Para remover o valor `1`, escreva `lista.remove(Integer.valueOf(1))`.

**`Arrays.asList` e `List.of` não aceitam `add`:**

```java
List<String> l = Arrays.asList("a", "b");
l.set(0, "z");      // ok: altera no lugar
l.add("c");         // UnsupportedOperationException
List.of("a").add("b");   // UnsupportedOperationException — imutável
```

**Elementos sem `equals` não são encontrados:**

```java
class Pessoa { final String nome; Pessoa(String nome) { this.nome = nome; } }
List<Pessoa> pessoas = new ArrayList<>(List.of(new Pessoa("Ana")));
pessoas.contains(new Pessoa("Ana"));   // false, a menos que Pessoa implemente equals
pessoas.remove(new Pessoa("Ana"));     // também false — o objeto não "bate"
```

**Remover enquanto percorre:**

```java
List<String> nomes = new ArrayList<>(List.of("", "ok"));
// for (String s : nomes) if (s.isEmpty()) nomes.remove(s);  // ConcurrentModificationException
nomes.removeIf(String::isEmpty);   // forma segura de filtrar e remover
System.out.println(nomes);         // [ok]
```

## Profundidade

**Complexidade (ArrayList):** `get`/`set` são O(1) (acesso a array), `add` no fim é O(1) amortizado, `add`/`remove` no meio são O(n) porque deslocam os elementos. `LinkedList` inverte o jogo: `get` vira O(n) e a lista paga referências por nó.

**`equals` na prática:** `contains`, `indexOf` e `remove(Object)` comparam com `equals` (e no `ArrayList` percorrem a lista toda). Listas usadas como chave de mapa ou dentro de `HashSet` precisam de `hashCode` coerente com `equals`.

**Concorrência (fail-fast):** os iterators lançam `ConcurrentModificationException` se a lista mudar durante a iteração. Para leituras concorrentes, `CopyOnWriteArrayList` (cópia a cada escrita) ou uma cópia explícita dos dados.

**`List.of` vs `List.copyOf`:** `List.of(...)` aceita varargs e rejeita `null`; `List.copyOf(colecao)` copia uma coleção existente. Ambas devolvem listas imutáveis — boas para constantes e para retornos que não devem ser alterados pelo chamador.

**Covariancia de array:** `String[]` é um `Object[]`, mas `List<String>` **não** é um `List<Object>` — isso protege contra `ClassCastException` em tempo de execução e é um dos motivos de existir genéricos (JLS §4.10.2).
