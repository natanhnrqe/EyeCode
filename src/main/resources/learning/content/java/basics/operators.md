---
id: java/basics/operators
title: Operadores em Java
concept: operators
level: beginner
duration: 7
category: FUNDAMENTOS
related:
  - java/basics/variables
  - java/basics/primitive-types
  - java/syntax/types/instanceof
  - java/syntax/control-flow/if
  - java/syntax/control-flow/for
---
Operadores combinam valores e produzem resultados. Em vez de decorar uma lista enorme, é mais útil reconhecer os grupos e usar parênteses quando a intenção não for óbvia.

```java
int sum = 2 + 3;                 // aritmético
sum += 4;                       // atribuição
boolean valid = sum >= 5;       // comparação
boolean allowed = valid && ready; // lógico
int next = ++sum;               // unário e incremento
int label = valid ? 1 : 0;      // ternário
int bits = (1 << 3) | 1;        // deslocamento e bitwise
```

Os operadores aritméticos incluem `+`, `-`, `*`, `/` e `%`. Atribuições compostas, como `+=`, combinam uma operação com a atribuição. Comparações produzem `boolean`, por exemplo `==`, `!=`, `<` e `>=`.

`!`, `++` e `--` são operadores unários. `&&` e `||` combinam condições com avaliação de curto-circuito. Operadores bitwise (`&`, `|`, `^`, `~`) e de deslocamento (`<<`, `>>`, `>>>`) trabalham com representações inteiras.

`instanceof` testa a compatibilidade de uma referência com um tipo. O operador ternário escolhe entre dois valores e deve ser usado apenas quando a expressão continuar legível.

A precedência define a ordem de avaliação, mas parênteses explícitos costumam comunicar melhor a intenção e evitam enganos durante a manutenção.