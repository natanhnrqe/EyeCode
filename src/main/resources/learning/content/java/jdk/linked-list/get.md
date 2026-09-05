---
id: java/jdk/linked-list/get
title: LinkedList.get()
concept: linked-list-get
kind: method
sourceMember: get
sourceSignature: (int)
level: beginner
duration: 1
category: COLEÇÕES
depth: quick
related:
  - java/jdk/linked-list
parent: java/jdk/linked-list
---
## O que ele faz?

Retorna o elemento de um índice.

```java
LinkedList<String> list = new LinkedList<>();
String value = list.get(2);
```

O acesso por índice exige percurso, diferente de uma busca equivalente em `ArrayList`.
