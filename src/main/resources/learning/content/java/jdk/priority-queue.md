---
id: java/jdk/priority-queue
title: PriorityQueue
concept: priority-queue
level: beginner
duration: 6
category: COLEÇÕES
officialDocs:
  label: PriorityQueue
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/PriorityQueue.html
related:
  - java/jdk/queue
  - java/jdk/comparator
  - java/jdk/comparable
  - java/collections/choosing
---
## O que é isso?

`PriorityQueue` organiza a cabeça da fila pela menor prioridade segundo a ordenação natural ou um `Comparator`. Ela não é uma fila FIFO comum.

```java
Queue<Integer> fila = new PriorityQueue<>();
fila.offer(30);
fila.offer(10);
fila.offer(20);

System.out.println(fila.poll());
System.out.println(fila.poll());
```

`poll` devolve os valores em ordem de prioridade. Porém, percorrer a fila com `for` não promete uma sequência completamente ordenada; use `poll` para consumir segundo a prioridade.
