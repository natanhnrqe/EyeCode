---
id: java/jdk/linked-hash-set
title: LinkedHashSet
concept: linked-hash-set
level: beginner
duration: 5
category: COLEÇÕES
officialDocs:
  label: LinkedHashSet
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/LinkedHashSet.html
related:
  - java/jdk/set
  - java/jdk/hash-set
  - java/jdk/tree-set
  - java/collections/choosing
---
## O que é isso?

`LinkedHashSet` combina a verificação de duplicidade de um conjunto baseado em hashing com a manutenção da ordem de encontro dos elementos em usos normais.

```java
Set<String> linguagens = new LinkedHashSet<>();
linguagens.add("Java");
linguagens.add("Kotlin");
linguagens.add("Java");

for (String linguagem : linguagens) {
    System.out.println(linguagem);
}
```

Assim, o exemplo percorre `Java` e depois `Kotlin`, sem repetir `Java`. A ordem preservada não transforma a estrutura em uma lista nem elimina a importância de `equals` e `hashCode`.
