---
id: java/syntax/exceptions/catch
title: catch
concept: catch
level: beginner
duration: 2
category: EXCEÇÕES
depth: quick
related:
  - java/syntax/exceptions/try
  - java/syntax/exceptions/finally
  - java/exceptions/multiple-catch
  - java/exceptions/checked-unchecked
---
catch recebe o objeto lançado por um try e define como tratar uma exceção compatível.

```java
try {
    int numero = Integer.parseInt("abc");
} catch (NumberFormatException erro) {
    System.out.println(erro.getMessage());
}
```

Quando há vários catch, coloque os tipos mais específicos antes dos mais gerais.
