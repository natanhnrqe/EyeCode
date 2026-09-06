---
id: java/jdk/string-builder/replace
title: StringBuilder.replace()
concept: string-builder-replace
kind: method
sourceMember: replace
level: beginner
duration: 2
category: API JAVA
parent: java/jdk/string-builder
related:
  - java/jdk/string-builder
---
## O que ele faz?

Usa replace() em StringBuilder para realizar a operação indicada pela API Java.

```java
StringBuilder texto = new StringBuilder("EyeCode");
texto.replace(0, 2, "Java");
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de StringBuilder. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
