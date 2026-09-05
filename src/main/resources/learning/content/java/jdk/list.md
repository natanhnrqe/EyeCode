---
id: java/jdk/list
title: List
concept: list
level: beginner
duration: 4
category: COLEÇÕES
officialDocs:
  label: List
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/List.html
related:
  - java/jdk/array-list
---
## O que é isso?

`List` é uma coleção ordenada que pode conter valores repetidos.

```java
List<String> names = new ArrayList<>();
names.add("Ada");
```

Use `List` quando a ordem dos elementos fizer parte do contrato e quando a implementação concreta puder variar.
