---
id: java/jdk/throwable
title: Throwable
concept: throwable
level: beginner
duration: 4
category: API JAVA
officialDocs:
  label: Throwable
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Throwable.html
related:
  - java/jdk/exception
  - java/jdk/runtime-exception
  - java/jdk/error
  - java/syntax/exceptions/throw
  - java/syntax/exceptions/catch
---
Throwable é a raiz dos objetos que participam de throw e catch. Seus ramos principais são Exception e Error.

```java
Throwable problema = new Exception("Falha ao carregar");
String mensagem = problema.getMessage();
Throwable causa = problema.getCause();

System.out.println(mensagem);
System.out.println(causa);
```

A hierarquia é Object, Throwable, Exception ou Error. Aplicações normalmente lançam subclasses específicas, em vez de estender Throwable diretamente.
