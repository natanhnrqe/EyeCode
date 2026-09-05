---
id: java/syntax/exceptions/finally
title: finally
concept: finally
level: beginner
duration: 2
category: EXCEÇÕES
depth: quick
related:
  - java/syntax/exceptions/try
  - java/syntax/exceptions/catch
  - java/exceptions/try-with-resources
---
finally é executado depois da sequência try/catch em fluxos normais e excepcionais, sendo útil para limpeza simples.

```java
try {
    System.out.println("processando");
} catch (RuntimeException erro) {
    System.out.println("falhou");
} finally {
    System.out.println("limpeza");
}
```

Não trate finally como garantia absoluta: encerramento do processo ou outras condições podem impedir a execução normal. Para AutoCloseable, prefira try-with-resources.
