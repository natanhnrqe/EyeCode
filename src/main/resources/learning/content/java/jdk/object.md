---
id: java/jdk/object
title: Object
concept: object
level: beginner
duration: 4
category: API JAVA
members:
- toString(): java/jdk/object/to-string
- equals(): java/jdk/object/equals
- hashCode(): java/jdk/object/hash-code
- getClass(): java/jdk/object/get-class
- clone(): java/jdk/object/clone
- finalize(): java/jdk/object/finalize
officialDocs:
  label: Object
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html
related:
  - java/types/class
---
## O que é isso?

`Object` é a raiz da hierarquia de classes Java. Toda classe herda métodos como `toString`, `equals` e `hashCode`.

Sobrescreva esses métodos quando o tipo precisar representar corretamente seu valor ou identidade.
