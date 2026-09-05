---
id: java/basics/comments
title: Comentários em Java
concept: comments
level: beginner
duration: 3
category: FUNDAMENTOS
related:
  - java/basics/source-file
  - java/basics/identifiers
  - java/types/class
---
Comentários explicam o código para quem lê, mas não fazem parte da lógica executável do programa.

## Comentário de linha

Use `//` para comentar o restante da linha:

```java
int age = 20; // idade inicial
```

## Comentário de bloco

Use `/*` e `*/` para delimitar um comentário que pode ocupar várias linhas:

```java
/*
 * Esta regra vale para o cadastro inteiro.
 */
boolean active = true;
```

## Documentação com Javadoc

Um comentário iniciado por `/**` é um comentário de documentação. Ele pode ser associado a uma classe, método ou campo e depois processado por ferramentas de documentação:

```java
/** Retorna o total de itens disponíveis. */
int count() {
    return 3;
}
```

Javadoc é uma forma estruturada de documentação, mas continua sem executar como instrução Java. Não confunda uma explicação no comentário com uma validação feita pelo programa.