---
id: java/jdk/error
title: Error
concept: error
level: beginner
duration: 3
category: API JAVA
officialDocs:
  label: Error
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Error.html
related:
  - java/jdk/throwable
  - java/jdk/exception
  - java/syntax/exceptions/catch
---
Error é um ramo de Throwable geralmente associado a falhas graves da JVM, do ambiente ou da infraestrutura.

```java
Error erro = new AssertionError("Estado impossível");

System.out.println(erro instanceof Throwable);
```

Aplicações comuns não devem usar Error para representar falhas esperadas de negócio. Alguns componentes especializados interagem com Errors, mas capturá-los como recuperação geral não é normal.
