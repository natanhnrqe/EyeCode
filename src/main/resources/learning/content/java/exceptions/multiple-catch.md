---
id: java/exceptions/multiple-catch
title: Múltiplos catch
concept: multiple-catch
level: beginner
duration: 3
category: EXCEÇÕES
related:
  - java/syntax/exceptions/try
  - java/syntax/exceptions/catch
  - java/exceptions/checked-unchecked
---
É possível tratar tipos diferentes com blocos catch separados ou com multi-catch usando |.

```java
try {
    int numero = Integer.parseInt("abc");
} catch (NumberFormatException erro) {
    System.out.println("número inválido");
} catch (RuntimeException erro) {
    System.out.println("falha de execução");
}
```

Blocos separados permitem comportamentos distintos. Multi-catch compartilha o tratamento:

```java
try {
    carregar();
} catch (java.io.IOException | IllegalStateException erro) {
    System.out.println(erro.getMessage());
}
```

Tipos mais específicos devem aparecer antes dos mais gerais nos blocos separados.
