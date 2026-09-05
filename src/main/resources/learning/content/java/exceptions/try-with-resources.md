---
id: java/exceptions/try-with-resources
title: try-with-resources
concept: try-with-resources
level: beginner
duration: 4
category: EXCEÇÕES
related:
  - java/jdk/auto-closeable
  - java/syntax/exceptions/try
  - java/syntax/exceptions/finally
---
try-with-resources declara recursos no cabeçalho do try e fecha automaticamente objetos AutoCloseable.

```java
class Recurso implements AutoCloseable {
    void usar() {
        System.out.println("usando");
    }

    @Override
    public void close() {
        System.out.println("fechando");
    }
}

try (Recurso recurso = new Recurso()) {
    recurso.usar();
}
```

O close ocorre ao sair do bloco, inclusive por exceção. Com vários recursos, o fechamento ocorre na ordem inversa da declaração.
