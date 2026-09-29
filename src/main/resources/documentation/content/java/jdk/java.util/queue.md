---
id: java/jdk/java.util/queue
title: Queue e Deque
type: api
summary: Filas e duplamente terminais no Java, com ArrayDeque na prática, métodos offer poll peek e a diferença entre versões que devolvem null e as que lançam exceção.
level: beginner
duration: 7
officialDocs:
  label: API java.util.Queue
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Queue.html
related:
  - java/jdk/java.util/list
  - java/jdk/java.util/collections
  - java/jdk/java.util/optional
---

> [!INFO] `Queue` é **FIFO** (entra no fim, sai no início) e `Deque` é duplamente terminal (entra e sai pelos dois lados). Implementação padrão: **`ArrayDeque`** — rápido nas duas pontas e serve de fila e de pilha ao mesmo tempo.

## Visão geral

Fila modela "atendimento na ordem de chegada": jobs, mensagens, BFS. `Deque` (double-ended queue) permite inserir/remover nas duas pontas, o que o torna também a estrutura certa para **pilha (stack)** — substituindo `Stack` (antiga) e `LinkedList`.

```java
Queue<String> fila = new ArrayDeque<>();      // fila clássica
Deque<String> dupla = new ArrayDeque<>();     // fila + pilha
Deque<String> pilha = new ArrayDeque<>();     // pilha: push/pop no início
PriorityQueue<Integer> prioridade = new PriorityQueue<>();  // menor valor primeiro
```

`PriorityQueue` **não** é FIFO — ela entrega sempre o menor elemento segundo a ordem natural ou o `Comparator`.

## Assinaturas

| Método | Retorna | O que faz |
|--------|---------|-----------|
| `offer(E e)` | `boolean` | adiciona; `false` se não couber |
| `poll()` | `E` | remove e devolve o início; `null` se vazia |
| `peek()` | `E` | lê o início sem remover; `null` se vazia |
| `add(E e)` | `boolean` | adiciona; lança exceção se falhar |
| `remove()` | `E` | remove o início; lança exceção se vazia |
| `element()` | `E` | lê o início; lança exceção se vazia |
| `size()` / `isEmpty()` | `int` / `boolean` | tamanho / vazio |
| `addFirst(E)` / `addLast(E)` | `void` | (Deque) insere nas pontas |
| `offerFirst(E)` / `offerLast(E)` | `boolean` | (Deque) insere, devolve `false` se falhar |
| `pollFirst()` / `pollLast()` | `E` | (Deque) remove nas pontas; `null` se vazia |
| `peekFirst()` / `peekLast()` | `E` | (Deque) lê nas pontas; `null` se vazia |
| `push(E)` / `pop()` | `void` / `E` | (Deque) pilha no início |

`push` equivale a `addFirst`, `pop` equivale a `removeFirst` e `peek()` de `Deque` equivale a `peekFirst()`.

## Enfileirar e desenfileirar

```java
Queue<String> fila = new ArrayDeque<>();
fila.offer("ana");                 // true
fila.offer("bia");
fila.offer("caio");
System.out.println(fila.peek());   // ana (não remove)
System.out.println(fila.poll());   // ana
System.out.println(fila.poll());   // bia
System.out.println(fila.poll());   // caio
System.out.println(fila.poll());   // null — fila vazia, sem exceção
// Padrão clássico: while ((x = fila.poll()) != null) { ... }
```

Armadilha: as duas famílias convivem — `offer`/`poll`/`peek` **devolvem `null`**; `add`/`remove`/`element` **lançam exceção** na fila vazia. Misturar as duas confunde o leitor.

## Duas pontas com Deque

```java
Deque<String> d = new ArrayDeque<>();
d.addLast("meio");        // [meio]
d.addFirst("inicio");     // [inicio, meio]
d.addLast("fim");         // [inicio, meio, fim]
System.out.println(d.peekFirst());  // inicio
System.out.println(d.peekLast());   // fim
d.pollLast();             // fim -> [inicio, meio]
d.pollFirst();            // inicio -> [meio]
// Fila dupla: entra por um lado, sai pelo outro (buffer, histórico de navegação)
```

**Armadilha:** `ArrayDeque` **não aceita `null`** — `addLast(null)` lança `NullPointerException`, diferente de `LinkedList`.

## Como pilha (stack)

```java
Deque<String> pilha = new ArrayDeque<>();
pilha.push("a");          // [a]
pilha.push("b");          // [a, b]
pilha.push("c");          // [a, b, c]
System.out.println(pilha.pop());   // c — LIFO: o último a entrar é o primeiro a sair
System.out.println(pilha.peek());  // b
System.out.println(pilha);        // [a, b]
// Para pilha, o topo fica no INÍCIO: push = addFirst, pop = removeFirst, peek = peekFirst
```

**`ArrayDeque` é a implementação recomendada** para pilha — `Vector`/`Stack` são legadas e `LinkedList` paga custo de nó por elemento.

## Armadilhas comuns

> [!WARNING] Em fila vazia, `poll()`/`peek()` devolvem `null` e `remove()`/`element()` lançam `NoSuchElementException`. Escolha UMA família e escreva o teste de vazio correspondente — `fila.remove()` dentro de um `while (!fila.isEmpty())` é seguro; fora dele, estoura.

**`PriorityQueue` não mantém a ordem de chegada:**

```java
Queue<Integer> p = new PriorityQueue<>();
p.addAll(List.of(5, 1, 3));
System.out.println(p.poll());   // 1 (menor primeiro), não 5
System.out.println(p);          // só os restantes (ex.: [3, 5]) — ordem de iteração NÃO é a de chegada
```

**`LinkedList` como fila é lenta e desnecessária:**

```java
Queue<String> ruim = new LinkedList<>();   // funciona, mas aloca um nó por elemento
Queue<String> bom  = new ArrayDeque<>();   // array contínuo, cache-friendly
// LinkedList também implementa Deque — a escolha só se justifica se você
// realmente fizer muitas inserções no meio (fora isso, ArrayList/ArrayDeque vencem)
```

**Esquecer de esvaziar na iteração:**

```java
while (!fila.isEmpty()) {
    // fila.peek();           // ERRADO: lê sem remover — o laço nunca termina
    String item = fila.poll(); // certo: remove e faz o laço avançar
}
```

## Profundidade

**Complexidade:** `ArrayDeque` é um array circular (dois ponteiros, head/tail) — `offer`/`poll`/`peek` são O(1) amortizado nas duas pontas e dobram de tamanho sob demanda. Não aceita elementos `null` (diferente de `LinkedList`), o que elimina ambiguidade de "vazio".

**Contrato da Queue (API):** `offer` é a operação "tenta inserir" (devolve `false` quando a capacidade é finita — ex.: `ArrayBlockingQueue`); `add` é herança de `Collection` e lança `IllegalStateException`. Em filas infinitas os dois fazem o mesmo.

**`PriorityQueue` é heap:** a raiz é sempre o menor elemento; `poll` extrai em O(log n), mas **não** entrega a fila inteira ordenada — para isso, copie para uma `List` e chame `sort`.

**Concorrência:** `ArrayDeque` não é thread-safe. Para produtor/consumidor, use `LinkedBlockingQueue`, `ArrayBlockingQueue` ou `ConcurrentLinkedQueue` — elas bloqueiam (ou spinam) de forma correta em vez de devolver `null`.

**`Deque` no lugar de pilha:** a própria API do JDK aponta `Deque` como estrutura de pilha — `Stack` é legado (sincroniza tudo sem necessidade) e `Vector` paga o mesmo custo por nó.
