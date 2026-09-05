---
id: java/syntax/exceptions/throws
title: throws
concept: throws
level: beginner
duration: 2
category: EXCEÇÕES
depth: quick
related:
  - java/syntax/exceptions/throw
  - java/syntax/exceptions/try
  - java/exceptions/checked-unchecked
  - java/exceptions/custom
---
throws declara na assinatura os tipos de exceção que um método pode repassar ao chamador.

```java
static String carregar() throws java.io.IOException {
    return java.nio.file.Files.readString(java.nio.file.Path.of("dados.txt"));
}
```

throw é a ação de lançar; throws é a declaração. Para exceções checked, o chamador deve tratar ou declarar a exceção conforme as regras do compilador.
