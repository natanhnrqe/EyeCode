---
id: java/exceptions/checked-unchecked
title: Exceções checked e unchecked
concept: checked-unchecked
level: beginner
duration: 4
category: EXCEÇÕES
related:
  - java/jdk/exception
  - java/jdk/runtime-exception
  - java/jdk/error
  - java/syntax/exceptions/throws
---
Exceções checked são subclasses de Exception fora da família RuntimeException. O compilador exige tratamento ou declaração.

```java
static void ler() throws java.io.IOException {
    java.nio.file.Files.readString(java.nio.file.Path.of("dados.txt"));
}
```

RuntimeException e suas subclasses são unchecked; Error e suas subclasses também não exigem declaração.

```java
static void validar(int valor) {
    if (valor < 0) {
        throw new IllegalArgumentException("valor inválido");
    }
}
```

Checked não significa necessariamente mais grave. A escolha depende de como o chamador pode reagir.
