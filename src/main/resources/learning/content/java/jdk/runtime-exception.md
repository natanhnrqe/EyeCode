---
id: java/jdk/runtime-exception
title: RuntimeException
concept: runtime-exception
level: beginner
duration: 4
category: API JAVA
officialDocs:
  label: RuntimeException
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/RuntimeException.html
related:
  - java/jdk/exception
  - java/jdk/throwable
  - java/syntax/exceptions/throw
  - java/syntax/exceptions/catch
---
RuntimeException é a família de exceções não verificadas. O compilador normalmente não exige declaração ou captura.

```java
static void definirIdade(int idade) {
    if (idade < 0) {
        throw new IllegalArgumentException("Idade inválida");
    }
}

definirIdade(-1);
```

NullPointerException, IllegalArgumentException e IndexOutOfBoundsException são exemplos. Não verificada não significa inofensiva; trate quando houver recuperação adequada.
