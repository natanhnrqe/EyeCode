---
id: java/basics/primitive-types
title: Tipos primitivos em Java
concept: primitive-types
level: beginner
duration: 6
category: FUNDAMENTOS
related:
  - java/basics/variables
  - java/basics/reference-types
  - java/basics/literals
  - java/basics/type-conversion
  - java/jdk/integer
---
Tipos primitivos representam valores diretamente, sem serem referências para objetos. Java possui oito tipos primitivos.

| Família | Tipos | Uso comum |
| --- | --- | --- |
| Inteiros | `byte`, `short`, `int`, `long` | Quantidades sem parte fracionária |
| Ponto flutuante | `float`, `double` | Valores aproximados com parte fracionária |
| Caractere | `char` | Um caractere UTF-16 |
| Lógico | `boolean` | `true` ou `false` |

```java
byte level = 3;
int age = 20;
long population = 8_000_000_000L;
double price = 19.90;
char initial = 'A';
boolean active = true;
```

`int` é a escolha comum para inteiros. `long` deve ser usado quando a faixa de valores pode exceder `int`; o sufixo `L` torna a intenção explícita. `double` normalmente oferece mais precisão que `float`, mas ambos representam aproximações, não todos os números reais exatamente.

`char` representa uma unidade de código UTF-16, não necessariamente um caractere visual completo. `boolean` representa uma condição lógica e não deve ser confundido com um número.

Valores primitivos não podem ser `null`. Quando uma API exige um objeto, o Java pode usar classes wrapper, como `Integer`; isso é diferente do tipo primitivo `int`.