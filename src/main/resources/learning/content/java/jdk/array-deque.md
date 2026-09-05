---
id: java/jdk/array-deque
title: ArrayDeque
concept: array-deque
level: beginner
duration: 6
category: COLEÇÕES
officialDocs:
  label: ArrayDeque
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ArrayDeque.html
related:
  - java/jdk/deque
  - java/jdk/queue
  - java/jdk/linked-list
  - java/collections/choosing
---
## O que é isso?

`ArrayDeque` é uma implementação comum para filas, deques e usos semelhantes a pilhas. Ela é usada através das interfaces `Deque` ou `Queue`.

```java
Deque<String> fila = new ArrayDeque<>();
fila.addLast("A");
fila.addLast("B");

System.out.println(fila.removeFirst());
```

Como pilha:

```java
Deque<String> pilha = new ArrayDeque<>();
pilha.push("A");
pilha.push("B");
System.out.println(pilha.pop());
```

Escolha a operação que deixa explícita a extremidade usada. Não crie um card `Stack` neste batch: `Deque` é a alternativa moderna para esse modelo.
