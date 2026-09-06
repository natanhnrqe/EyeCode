---
id: java/jdk/collection
title: Collection
concept: collection
level: beginner
duration: 6
category: COLEÇÕES
members:
- add(): java/jdk/collection/add
- addAll(): java/jdk/collection/add-all
- remove(): java/jdk/collection/remove
- removeAll(): java/jdk/collection/remove-all
- retainAll(): java/jdk/collection/retain-all
- contains(): java/jdk/collection/contains
- containsAll(): java/jdk/collection/contains-all
- size(): java/jdk/collection/size
- isEmpty(): java/jdk/collection/is-empty
- clear(): java/jdk/collection/clear
- toArray(): java/jdk/collection/to-array
- iterator(): java/jdk/collection/iterator
officialDocs:
  label: Collection
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collection.html
related:
  - java/collections/framework
  - java/jdk/iterable
  - java/jdk/list
  - java/jdk/set
  - java/jdk/queue
  - java/generics/generics
---
## O que é isso?

`Collection<E>` é a interface central para grupos de elementos. Ela define operações comuns, enquanto `List`, `Set` e `Queue` especializam contratos diferentes.

```java
Collection<String> nomes = new ArrayList<>();

nomes.add("Ana");
nomes.add("Bruno");

System.out.println(nomes.contains("Ana"));
System.out.println(nomes.size());
```

Programar contra `Collection` deixa o código depender do contrato necessário, e não de uma implementação específica.

## Operações comuns

`add`, `remove`, `contains`, `size`, `isEmpty` e `clear` aparecem em muitas coleções. O valor retornado por `add` ou `remove` informa se a coleção realmente mudou.

```java
nomes.remove("Bruno");
if (!nomes.isEmpty()) {
    System.out.println(nomes.iterator().next());
}
nomes.clear();
```

Uma `Collection` também é `Iterable`, por isso pode participar de um enhanced for-loop. A ordem, a duplicidade e o custo das operações dependem da subinterface e da implementação escolhida.
