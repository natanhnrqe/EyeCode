---
id: java/jdk/auto-closeable
title: AutoCloseable
concept: auto-closeable
level: beginner
duration: 4
category: API JAVA
officialDocs:
  label: AutoCloseable
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/AutoCloseable.html
related:
  - java/syntax/exceptions/try
  - java/jdk/exception
  - java/basics/reference-types
---
AutoCloseable define close() para recursos usados com try-with-resources.

```java
class Recurso implements AutoCloseable {
    void usar() {
        System.out.println("Usando recurso");
    }

    @Override
    public void close() {
        System.out.println("Fechando recurso");
    }
}

try (Recurso recurso = new Recurso()) {
    recurso.usar();
}
```

O close ocorre ao sair do bloco, inclusive quando uma exceção interrompe a execução. O próprio recurso deve liberar seus recursos em close().
