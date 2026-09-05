---
id: java/jdk/linked-list/get-first
title: LinkedList.getFirst()
concept: linked-list-get-first
kind: method
sourceMember: getFirst
sourceSignature: ()
level: beginner
duration: 1
category: COLEÇÕES
depth: quick
related:
  - java/jdk/linked-list
parent: java/jdk/linked-list
---
## O que ele faz?

Retorna o primeiro elemento.

```java
LinkedList<String> list = new LinkedList<>();
String first = list.getFirst();
```

O método lança uma exceção quando a lista está vazia; verifique o estado quando necessário.
