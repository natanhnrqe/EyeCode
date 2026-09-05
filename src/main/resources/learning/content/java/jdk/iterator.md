---
id: java/jdk/iterator
title: Iterator
concept: iterator
level: beginner
duration: 5
category: COLEÇÕES
officialDocs:
  label: Iterator
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Iterator.html
related:
  - java/jdk/iterable
  - java/jdk/collection
  - java/syntax/control-flow/for
---
## O que é isso?

`Iterator<E>` percorre uma coleção passo a passo. `hasNext()` verifica se existe outro elemento e `next()` avança e retorna o próximo.

```java
Iterator<String> iterator = nomes.iterator();
while (iterator.hasNext()) {
    String nome = iterator.next();
    System.out.println(nome);
}
```

## Remoção durante a iteração

Quando o iterador suporta remoção, `remove()` remove o último elemento devolvido por `next()`.

```java
Iterator<String> iterator = nomes.iterator();
while (iterator.hasNext()) {
    if (iterator.next().isBlank()) {
        iterator.remove();
    }
}
```

Nem todo iterador permite `remove`; a operação pode lançar `UnsupportedOperationException`. O enhanced for-loop é mais simples quando não é necessário controlar explicitamente o percurso.
