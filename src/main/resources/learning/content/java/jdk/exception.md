---
id: java/jdk/exception
title: Exception
concept: exception
level: beginner
duration: 4
category: API JAVA
officialDocs:
  label: Exception
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Exception.html
related:
  - java/jdk/throwable
  - java/jdk/runtime-exception
  - java/syntax/exceptions/try
  - java/syntax/exceptions/catch
  - java/syntax/exceptions/throws
---
Exception é um ramo de Throwable para condições que uma aplicação pode tratar. Exceções verificadas exigem tratamento ou declaração; RuntimeException e subclasses são não verificadas.

```java
static void carregar() throws Exception {
    throw new Exception("Falha ao carregar");
}

try {
    carregar();
} catch (Exception erro) {
    System.out.println(erro.getMessage());
}
```

Na prática, prefira tipos de exceção específicos. RuntimeException também é Exception, mas não é checked.
