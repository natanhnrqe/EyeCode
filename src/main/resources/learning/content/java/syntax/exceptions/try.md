---
id: java/syntax/exceptions/try
title: try
concept: try
level: beginner
duration: 2
category: EXCEÇÕES
depth: quick
related:
  - java/syntax/exceptions/catch
  - java/syntax/exceptions/finally
  - java/exceptions/try-with-resources
---
try delimita código cuja execução pode terminar normalmente ou lançar uma exceção.

```java
try {
    int numero = Integer.parseInt("abc");
} catch (NumberFormatException erro) {
    System.out.println("Número inválido");
}
```

O bloco catch trata uma exceção compatível lançada dentro do try.
