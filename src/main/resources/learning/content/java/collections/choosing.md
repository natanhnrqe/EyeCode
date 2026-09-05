---
id: java/collections/choosing
title: Escolhendo uma coleção
concept: choosing-collection
level: beginner
duration: 7
category: COLEÇÕES
related:
  - java/collections/framework
  - java/jdk/list
  - java/jdk/set
  - java/jdk/map
  - java/jdk/queue
  - java/jdk/comparator
---
## Comece pelo requisito

Use `List` e normalmente `ArrayList` quando precisar de uma sequência ordenada e acesso por índice. Use `Set` e `HashSet` quando a prioridade for unicidade; prefira `LinkedHashSet` quando a ordem de encontro também importar e `TreeSet` quando os elementos precisarem ficar ordenados.

Use `Map` e normalmente `HashMap` para associações chave-valor. `LinkedHashMap` ajuda quando a ordem de encontro deve ser preservada; `TreeMap` quando as chaves precisam permanecer ordenadas.

Para processamento FIFO, considere `Queue`, `Deque` e `ArrayDeque`. Para retirar o próximo elemento por prioridade, considere `PriorityQueue`.

Essas escolhas não são regras absolutas. Considere o contrato de ordenação, a semântica de igualdade, o tamanho dos dados e as operações realmente usadas. A implementação deve servir ao comportamento do programa, não apenas ao nome da estrutura.
