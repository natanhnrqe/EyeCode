---
id: java/basics/variables
title: Variáveis em Java
concept: variable
level: beginner
duration: 5
category: FUNDAMENTOS
officialDocs:
  label: Java Language Specification
  url: https://docs.oracle.com/javase/specs/
related:
  - java/types/class
next: java/types/class
---
Variáveis dão nome aos dados que um programa Java precisa lembrar. Uma variável tem um **tipo**, um nome e um valor.

## Declarando uma variável

Use um tipo como `int`, seguido de um nome descritivo:

```java
int score = 10;
String playerName = "Ada";
```

O valor de `score` pode mudar mais tarde no programa.

### Hábitos úteis

- Escolha nomes que expliquem os dados.
- Inicialize uma variável antes de lê-la.
- Mantenha o tipo coerente com o valor que precisa armazenar.

## Etapas

1. Escolha o valor que precisa armazenar.
2. Escolha o tipo Java desse valor.
3. Dê um nome claro à variável.

> Uma variável é um espaço identificado na memória. Seu tipo informa ao Java quais valores são válidos ali.

| Tipo | Exemplo | Uso comum |
| --- | --- | --- |
| `int` | `10` | Números inteiros |
| `String` | `"Ada"` | Texto |
| `boolean` | `true` | Sim ou não |

Leia mais na [especificação da linguagem Java](https://docs.oracle.com/javase/specs/).
