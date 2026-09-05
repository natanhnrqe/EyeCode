---
id: java/basics/type-conversion
title: Conversão de tipos
concept: type-conversion
level: beginner
duration: 5
category: FUNDAMENTOS
related:
  - java/basics/primitive-types
  - java/basics/reference-types
  - java/syntax/types/instanceof
  - java/basics/variables
---
Conversão de tipos adapta um valor para outro tipo compatível. Ela pode ser implícita, quando a conversão é considerada segura, ou explícita, quando o código precisa pedir um cast.

## Alargamento

Conversões numéricas para um tipo capaz de representar uma faixa maior costumam ocorrer automaticamente:

```java
int count = 10;
long total = count;
```

Nesse caso, o valor de `count` pode ser usado como `long` sem cast explícito.

## Narrowing e cast

Ao converter para um tipo menor, o programa pode perder informação. Use um cast explícito e deixe essa decisão visível:

```java
double price = 19.90;
int roundedDown = (int) price; // 19: a parte decimal é descartada
```

Um cast não arredonda automaticamente e não recupera a informação perdida. Verifique faixa e precisão antes de escolher a conversão.

Referências também podem ser convertidas dentro de uma hierarquia de tipos, mas o objeto precisa ser compatível com o tipo de destino. `instanceof` ajuda a testar essa compatibilidade antes de um cast quando necessário.

Boxing e unboxing são mecanismos relacionados, porém diferentes: eles tratam a passagem entre primitivos e objetos wrapper e serão estudados separadamente.