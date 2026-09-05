---
id: java/jdk/set
title: Set
concept: set
level: beginner
duration: 5
category: COLEÇÕES
officialDocs:
  label: Set
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Set.html
related:
  - java/collections/framework
  - java/jdk/collection
  - java/jdk/hash-set
  - java/jdk/linked-hash-set
  - java/jdk/tree-set
  - java/generics/generics
---
## O que é isso?

`Set<E>` representa elementos sem duplicidade segundo as regras de igualdade da implementação. Ele não oferece o modelo de acesso por índice de uma `List`.

```java
Set<String> linguagens = new HashSet<>();

System.out.println(linguagens.add("Java"));
System.out.println(linguagens.add("Java"));
System.out.println(linguagens.size());
```

A segunda chamada de `add` retorna `false` porque o conjunto não mudou. A ordem de iteração não é garantida por todo `Set`; escolha uma implementação que declare a ordenação necessária.

Use `contains` para testar pertencimento e `remove` para eliminar um elemento. `equals` e `hashCode` são fundamentais nas implementações baseadas em hashing.
