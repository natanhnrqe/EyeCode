---
id: java/jdk/deque
title: Deque
concept: deque
level: beginner
duration: 6
category: COLEÇÕES
officialDocs:
  label: Deque
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Deque.html
related:
  - java/jdk/queue
  - java/jdk/array-deque
  - java/jdk/linked-list
  - java/collections/choosing
---
## O que é isso?

`Deque<E>` é uma fila de duas extremidades. Ela pode modelar uma fila ou uma pilha sem recorrer à classe legada `Stack`.

```java
Deque<String> fila = new ArrayDeque<>();
fila.addFirst("primeiro");
fila.addLast("ultimo");

System.out.println(fila.removeFirst());
System.out.println(fila.removeLast());
```

Para cada extremidade existem pares como `addFirst`/`offerFirst`, `removeFirst`/`pollFirst` e `peekFirst`; o mesmo vale para o lado final. `push` e `pop` expressam o uso como pilha.
