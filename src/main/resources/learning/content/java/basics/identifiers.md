---
id: java/basics/identifiers
title: Identificadores em Java
concept: identifiers
level: beginner
duration: 4
category: FUNDAMENTOS
related:
  - java/basics/variables
  - java/basics/source-file
  - java/types/class
  - java/basics/primitive-types
  - java/basics/reference-types
---
Identificadores são os nomes usados para distinguir variáveis, métodos, classes, interfaces e outros elementos do código.

## Regras da linguagem

Java diferencia maiúsculas de minúsculas. `name`, `Name` e `NAME` são identificadores diferentes. Um identificador pode começar com uma letra, `_` ou `$`, e os caracteres seguintes podem incluir dígitos. Ele não pode ser uma palavra reservada usada como nome comum.

```java
int age = 20;
int 2items = 2;       // inválido: começa com número
int class = 1;        // inválido: class é palavra reservada
int userCount = 3;    // válido
```

## Convenções de nomes

Convenções ajudam a leitura, mas não são exigências do compilador:

- `camelCase` para variáveis e métodos, como `userCount`.
- `PascalCase` para classes, como `UserAccount`.
- `UPPER_SNAKE_CASE` para constantes, como `MAX_RETRIES`.

Escolha nomes que indiquem o papel do valor. Um nome claro reduz a necessidade de comentários e facilita a manutenção.