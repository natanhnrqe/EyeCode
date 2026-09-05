---
id: java/jdk/linked-list
title: LinkedList
concept: linked-list
level: beginner
duration: 8
category: COLEÇÕES
officialDocs:
  label: LinkedList
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/LinkedList.html
related:
  - java/jdk/array-list
  - java/jdk/list
members:
  - add(): java/jdk/linked-list/add
  - addFirst(): java/jdk/linked-list/add-first
  - addLast(): java/jdk/linked-list/add-last
  - getFirst(): java/jdk/linked-list/get-first
  - getLast(): java/jdk/linked-list/get-last
  - removeFirst(): java/jdk/linked-list/remove-first
  - removeLast(): java/jdk/linked-list/remove-last
  - get(): java/jdk/linked-list/get
  - size(): java/jdk/linked-list/size
---
## O que é isso?

Uma lista encadeada armazena uma cadeia de elementos em vez de um único array contíguo. A `LinkedList` do Java é duplamente encadeada: cada nó conceitual mantém um item, uma referência ao nó anterior e outra ao próximo.

```text
anterior <-> [item] <-> próximo
```

## Como funciona?

Para acessar um índice, a implementação percorre a lista a partir de uma das extremidades até chegar à posição solicitada.

## Operações e escolhas

Adicionar ou remover em uma extremidade conhecida é natural com `addFirst()`, `addLast()`, `removeFirst()` e `removeLast()`. Encontrar um índice arbitrário ainda exige percurso; `get(index)` não é uma operação de tempo constante como em um array. A implementação pode começar pela extremidade mais próxima, mas ainda precisa caminhar.

Cada elemento carrega o custo de seus links, e nós separados costumam ter pior localidade de cache que o armazenamento contíguo de `ArrayList`. Por isso, `LinkedList` não é automaticamente melhor para inserções ou remoções: se o código precisa procurar a posição antes, o percurso pode dominar o custo. Prefira a estrutura compatível com o padrão de acesso real.

## Membros úteis

`add()`, `addFirst()`, `addLast()`, `getFirst()`, `getLast()`, `removeFirst()`, `removeLast()`, `get()` e `size()` cobrem usos comuns. O tipo também implementa as abstrações `List` e `Deque`.
