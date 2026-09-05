---
id: java/jdk/collections
title: Collections
concept: collections-utility
level: beginner
duration: 6
category: COLEÇÕES
officialDocs:
  label: Collections
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collections.html
related:
  - java/jdk/collection
  - java/jdk/list
  - java/jdk/comparator
  - java/collections/choosing
---
## Collection e Collections

`Collection` é uma interface que representa grupos de elementos. `Collections` é uma classe utilitária com métodos estáticos para trabalhar com coleções, principalmente listas.

```java
List<String> nomes = new ArrayList<>();
nomes.add("Bruno");
nomes.add("Ana");

Collections.sort(nomes);
Collections.reverse(nomes);
System.out.println(nomes);
```

## Operações úteis

`sort`, `reverse`, `shuffle`, `min`, `max` e `frequency` resolvem operações comuns sem criar uma implementação nova.

```java
Collections.shuffle(nomes);
String menor = Collections.min(nomes);
int repeticoes = Collections.frequency(nomes, "Ana");
```

Também existem fábricas e wrappers imutáveis ou não modificáveis. `Collections.unmodifiableList` impede alterações por aquela referência, mas não transforma automaticamente os objetos internos em imutáveis.
