---
id: java/generics/type-erasure
title: Apagamento de tipos
concept: type-erasure
level: beginner
duration: 3
category: FUNDAMENTOS
related:
  - java/generics/generics
  - java/generics/generic-classes
  - java/generics/wildcards
---
Generics são usados principalmente pelo compilador e Java implementa a maior parte deles por apagamento de tipos em runtime.

Isso cria restrições importantes:

```java
class Caixa<T> {
    // new T();       // não é permitido
    // new T[10];     // não é permitido
}

// objeto instanceof java.util.List<String>; // não é permitido
```

O tipo concreto continua útil para a verificação durante a compilação, mas não está disponível da mesma forma em todas as operações em runtime.
