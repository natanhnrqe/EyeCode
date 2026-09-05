---
id: java/collections/framework
title: Collections Framework em Java
concept: collections-framework
level: beginner
duration: 7
category: COLEÇÕES
related:
  - java/jdk/iterable
  - java/jdk/collection
  - java/jdk/list
  - java/jdk/set
  - java/jdk/queue
  - java/jdk/map
---
## A ideia central

O Collections Framework reúne interfaces, implementações e algoritmos para trabalhar com grupos de dados. Interfaces definem contratos; implementações fornecem comportamentos concretos; generics expressam os tipos dos elementos, chaves e valores.

Uma visão útil é:

```text
Iterable
  Collection
    List   Set   Queue
                    Deque
```

`Map` participa do framework, mas não é subinterface de `Collection`: ele associa chaves a valores.

Escolha a abstração pelo comportamento necessário. `List` preserva uma sequência e aceita duplicatas; `Set` representa unicidade; `Queue` organiza retirada; `Map` resolve associações por chave. Depois escolha uma implementação compatível com ordem, desempenho e igualdade.
