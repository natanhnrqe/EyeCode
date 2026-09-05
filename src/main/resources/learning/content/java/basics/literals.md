---
id: java/basics/literals
title: Literais em Java
concept: literals
level: beginner
duration: 5
category: FUNDAMENTOS
related:
  - java/syntax/literals/true
  - java/syntax/literals/false
  - java/syntax/literals/null
  - java/basics/primitive-types
  - java/basics/variables
  - java/basics/type-conversion
---
Um literal é um valor escrito diretamente no código-fonte.

## Exemplos

```java
int count = 42;             // inteiro
double rate = 0.75;         // ponto flutuante
long total = 10_000L;       // inteiro long
float scale = 1.5F;         // float
char letter = 'A';          // caractere
String message = "Olá";    // texto
boolean ready = true;       // booleano
String missing = null;      // referência sem objeto
```

Inteiros podem usar separadores `_` para facilitar a leitura. `L` indica um literal `long`; `F` indica um literal `float`. Sem esses sufixos, números inteiros normalmente começam como `int` e números decimais como `double`.

Strings e caracteres usam aspas diferentes: strings usam aspas duplas e caracteres usam aspas simples. Sequências de escape representam caracteres especiais:

```java
String line = "primeira linha\nsegunda linha";
char quote = '\'';
```

`true`, `false` e `null` já possuem cards rápidos próprios. Aqui eles aparecem como parte do conjunto de formas pelas quais valores podem ser escritos no código.