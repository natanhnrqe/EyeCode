---
id: java/jdk/queue
title: Queue
concept: queue
level: beginner
duration: 5
category: COLEÇÕES
officialDocs:
  label: Queue
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Queue.html
related:
  - java/collections/framework
  - java/jdk/collection
  - java/jdk/deque
  - java/jdk/array-deque
  - java/jdk/priority-queue
---
## O que é isso?

`Queue<E>` representa uma fila. FIFO é o modelo comum, mas a interface não obriga todas as implementações a remover na ordem de inserção; `PriorityQueue` é a exceção importante.

```java
Queue<String> fila = new ArrayDeque<>();
fila.offer("Ana");
fila.offer("Bruno");

System.out.println(fila.peek());
System.out.println(fila.poll());
```

`add` e `remove` normalmente sinalizam falha com exceção. `offer` e `poll` usam valores de retorno especiais quando a operação não pode ser concluída. `element` e `peek` apenas consultam a cabeça, com políticas de falha diferentes.
