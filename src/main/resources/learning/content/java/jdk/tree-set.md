---
id: java/jdk/tree-set
title: TreeSet
concept: tree-set
level: beginner
duration: 6
category: COLEÇÕES
officialDocs:
  label: TreeSet
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/TreeSet.html
related:
  - java/jdk/set
  - java/jdk/comparable
  - java/jdk/comparator
  - java/collections/choosing
---
## O que é isso?

`TreeSet` é um `Set` ordenado e navegável. Os elementos usam a ordenação natural ou um `Comparator` fornecido ao conjunto.

```java
Set<Integer> numeros = new TreeSet<>();
numeros.add(30);
numeros.add(10);
numeros.add(20);

for (int numero : numeros) {
    System.out.println(numero);
}
```

Os valores são percorridos em ordem crescente. Métodos como `first()` e `last()` acessam as extremidades ordenadas.

Os elementos precisam ser compatíveis com a ordenação escolhida. Um `TreeSet` não usa `hashCode` para decidir a posição.
