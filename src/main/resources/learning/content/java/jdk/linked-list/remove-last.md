---
id: java/jdk/linked-list/remove-last
title: LinkedList.removeLast()
concept: linked-list-remove-last
kind: method
sourceMember: removeLast
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

Remove e retorna o último elemento.

```java
LinkedList<String> list = new LinkedList<>();
String last = list.removeLast();
```

O método lança uma exceção quando a lista está vazia.
