---
id: java/jdk/hash-set
title: HashSet
concept: hash-set
level: beginner
duration: 5
category: COLEÇÕES
officialDocs:
  label: HashSet
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/HashSet.html
related:
  - java/jdk/set
  - java/jdk/linked-hash-set
  - java/jdk/tree-set
  - java/jdk/object
  - java/collections/choosing
---
## O que é isso?

`HashSet` usa hashing para representar um `Set`. É uma escolha comum quando o objetivo principal é testar pertencimento ou eliminar duplicidades, sem exigir uma ordem de iteração.

```java
Set<String> linguagens = new HashSet<>();
linguagens.add("Java");
linguagens.add("Kotlin");
linguagens.add("Java");

if (linguagens.contains("Java")) {
    System.out.println("encontrada");
}
```

Não conte com uma ordem específica ao percorrer um `HashSet`. Para que a duplicidade seja reconhecida, os elementos precisam ter semântica coerente de `equals` e `hashCode`.
