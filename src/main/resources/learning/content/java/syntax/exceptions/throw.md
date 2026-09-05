---
id: java/syntax/exceptions/throw
title: throw
concept: throw
level: beginner
duration: 2
category: EXCEÇÕES
depth: quick
related:
  - java/syntax/exceptions/throws
  - java/syntax/exceptions/catch
  - java/exceptions/custom
---
throw realiza a ação de lançar um objeto compatível com Throwable.

```java
static void definirIdade(int idade) {
    if (idade < 0) {
        throw new IllegalArgumentException("Idade inválida");
    }
}
```

throw interrompe o fluxo atual e procura um catch compatível. Não confunda com throws, que aparece na assinatura e declara uma possibilidade.
